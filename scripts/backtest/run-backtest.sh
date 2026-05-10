#!/bin/bash
# Backtest the /api/v1/scan-v2 endpoint against scraped HelloFresh recipes.
#
# For each recipe in recipes/, send the photo to the VPS, parse the response,
# compare totals to the recipe's known per-serving macros, and write a CSV
# with row-level errors plus print aggregate MAPE per macro.
#
# Usage:
#   ./run-backtest.sh [N]   # run on first N recipes (default: all)
#
# Requires: ssh access to vps, sips (macOS), python3

set -eu

ROOT="$(cd "$(dirname "$0")" && pwd)"
RECIPES_DIR="$ROOT/recipes"
RESULTS_DIR="$ROOT/results"
TS=$(date +%Y%m%d-%H%M%S)
CSV="$RESULTS_DIR/backtest-$TS.csv"
LIMIT=${1:-9999}

mkdir -p "$RESULTS_DIR"

echo "slug,real_kcal,real_protein,real_fat,real_carbs,pred_kcal,pred_protein,pred_fat,pred_carbs,err_kcal,err_protein,err_fat,err_carbs,latency_s,http,gemini_items" > "$CSV"

# Run inside the container so API_KEY stays out of our shell.
RUNNER=$(mktemp -t backtest-runner-XXXX).sh
cat > "$RUNNER" <<'EOF'
#!/bin/sh
set -eu
B64=$(base64 -w0 /tmp/backtest-photo.jpg)
cat > /tmp/backtest-body.json <<JSON
{"images":["$B64"],"hint":""}
JSON
START=$(date +%s.%N)
HTTP=$(curl -s -X POST http://localhost:3000/api/v1/scan-v2 \
  -H "Content-Type: application/json" \
  -H "x-api-key: ${API_KEY}" \
  --data-binary @/tmp/backtest-body.json \
  -o /tmp/backtest-response.json \
  -w "%{http_code}")
END=$(date +%s.%N)
LATENCY=$(awk "BEGIN { printf \"%.2f\", $END - $START }")
echo "$HTTP|$LATENCY"
cat /tmp/backtest-response.json
EOF

scp -q "$RUNNER" vps:/tmp/backtest-runner.sh
ssh vps "docker cp /tmp/backtest-runner.sh nutrition-api-nutrition-api-1:/tmp/runner.sh >/dev/null"

count=0
for recipe in "$RECIPES_DIR"/*/; do
  if [ "$count" -ge "$LIMIT" ]; then break; fi
  slug=$(basename "$recipe")
  data="$recipe/data.json"
  photo="$recipe/photo.jpg"

  if [ ! -f "$data" ] || [ ! -f "$photo" ]; then continue; fi

  count=$((count + 1))

  # Read real macros from data.json
  real=$(python3 -c "
import json, sys
d = json.load(open('$data'))
n = d.get('nutrition_per_serving', {})
print(f\"{n.get('kcal') or 0}|{n.get('protein_g') or 0}|{n.get('fat_g') or 0}|{n.get('carbs_g') or 0}\")
")
  real_kcal=$(echo "$real" | cut -d'|' -f1)
  real_protein=$(echo "$real" | cut -d'|' -f2)
  real_fat=$(echo "$real" | cut -d'|' -f3)
  real_carbs=$(echo "$real" | cut -d'|' -f4)

  printf "[%3d] %s … " "$count" "$slug"

  # Resize photo to keep payload sane
  TMP=$(mktemp -t backtest-photo-XXXX).jpg
  sips -Z 1024 -s format jpeg -s formatOptions 80 "$photo" --out "$TMP" >/dev/null 2>&1 || {
    echo "resize failed"
    rm -f "$TMP"
    continue
  }

  scp -q "$TMP" vps:/tmp/backtest-photo.jpg
  rm -f "$TMP"

  # Send to API and parse
  output=$(ssh vps "docker cp /tmp/backtest-photo.jpg nutrition-api-nutrition-api-1:/tmp/backtest-photo.jpg >/dev/null && docker compose -f ~/nutrition-api/docker-compose.yml exec -T nutrition-api sh /tmp/runner.sh" 2>&1)

  meta=$(echo "$output" | head -n 1)
  body=$(echo "$output" | tail -n +2)
  http=$(echo "$meta" | cut -d'|' -f1)
  latency=$(echo "$meta" | cut -d'|' -f2)

  if [ "$http" != "200" ]; then
    echo "HTTP $http"
    printf "%s,%s,%s,%s,%s,,,,,,,,,%s,%s,\n" \
      "$slug" "$real_kcal" "$real_protein" "$real_fat" "$real_carbs" "$latency" "$http" >> "$CSV"
    sleep 1
    continue
  fi

  # Parse predicted totals + items list (with python for safety)
  parsed=$(echo "$body" | python3 -c "
import json, sys
d = json.load(sys.stdin)
t = d.get('totals', {})
items_summary = '|'.join(f\"{i['prediction']['name']}({i['prediction']['amount']}{i['prediction']['unit']})\" for i in d.get('items', []))
items_summary = items_summary.replace(',', ' ').replace('\"', '')
print(f\"{t.get('kcal',0)}|{t.get('protein',0)}|{t.get('fat',0)}|{t.get('carbs',0)}|{items_summary}\")
")
  pred_kcal=$(echo "$parsed" | cut -d'|' -f1)
  pred_protein=$(echo "$parsed" | cut -d'|' -f2)
  pred_fat=$(echo "$parsed" | cut -d'|' -f3)
  pred_carbs=$(echo "$parsed" | cut -d'|' -f4)
  items_list=$(echo "$parsed" | cut -d'|' -f5-)

  err=$(python3 -c "
def pct(p, r):
    if r == 0: return ''
    return f'{(p - r) / r * 100:+.1f}'
print(f\"{pct($pred_kcal,$real_kcal)}|{pct($pred_protein,$real_protein)}|{pct($pred_fat,$real_fat)}|{pct($pred_carbs,$real_carbs)}\")
")
  err_kcal=$(echo "$err" | cut -d'|' -f1)
  err_protein=$(echo "$err" | cut -d'|' -f2)
  err_fat=$(echo "$err" | cut -d'|' -f3)
  err_carbs=$(echo "$err" | cut -d'|' -f4)

  printf "kcal %4.0f→%4.0f (%s%%) prot %.1f→%.1f (%s%%)\n" \
    "$real_kcal" "$pred_kcal" "$err_kcal" "$real_protein" "$pred_protein" "$err_protein"

  printf "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n" \
    "$slug" "$real_kcal" "$real_protein" "$real_fat" "$real_carbs" \
    "$pred_kcal" "$pred_protein" "$pred_fat" "$pred_carbs" \
    "$err_kcal" "$err_protein" "$err_fat" "$err_carbs" \
    "$latency" "$http" "\"$items_list\"" >> "$CSV"
done

rm -f "$RUNNER"

echo ""
echo "=== AGGREGATE RESULTS (from $CSV) ==="
python3 - "$CSV" <<'PY'
import csv, sys, statistics

path = sys.argv[1]
errs = {"kcal": [], "protein": [], "fat": [], "carbs": []}
abs_errs = {"kcal": [], "protein": [], "fat": [], "carbs": []}
ok = 0
fail = 0
with open(path) as f:
    for row in csv.DictReader(f):
        if row["http"] != "200":
            fail += 1
            continue
        ok += 1
        for k in errs:
            v = row[f"err_{k}"].rstrip('%')
            if v == "" or v is None:
                continue
            try:
                x = float(v)
                errs[k].append(x)
                abs_errs[k].append(abs(x))
            except ValueError:
                pass

print(f"Recipes processed: {ok} OK, {fail} failed")
print()
print(f"{'macro':<10} {'MAPE':>8} {'mean err':>10} {'median err':>12} {'stdev':>8}")
for k in errs:
    if not errs[k]:
        continue
    mape = statistics.mean(abs_errs[k])
    mean = statistics.mean(errs[k])
    median = statistics.median(errs[k])
    sd = statistics.stdev(errs[k]) if len(errs[k]) > 1 else 0
    print(f"{k:<10} {mape:>7.1f}% {mean:>+9.1f}% {median:>+11.1f}% {sd:>7.1f}")
PY
echo ""
echo "Full CSV: $CSV"
