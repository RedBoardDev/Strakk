#!/bin/bash
# Quick local test of /api/v1/scan-v2 (Gemini) with a photo file.
#
# Usage:
#   ./test-meal-scan.sh <path-to-photo>
#
# Resizes the image to 1024px JPEG q=80, base64 encodes, posts via VPS,
# pretty-prints the result. Bypasses the iOS app entirely.

set -eu

if [ $# -lt 1 ]; then
  echo "Usage: $0 <path-to-photo>"
  exit 1
fi

PHOTO="$1"
if [ ! -f "$PHOTO" ]; then
  echo "Photo not found: $PHOTO"
  exit 1
fi

# Resize to keep payload < 5 MB
TMP_JPG=$(mktemp -t meal-scan-XXXX).jpg
sips -Z 1024 -s format jpeg -s formatOptions 80 "$PHOTO" --out "$TMP_JPG" >/dev/null

echo "Image resized → $(ls -lh "$TMP_JPG" | awk '{print $5}')"

# Run the curl from inside the VPS container so API_KEY stays out of the
# local shell. The container reads API_KEY from its env_file.
SCRIPT=$(mktemp -t meal-scan-script-XXXX).sh
cat > "$SCRIPT" <<'EOF'
#!/bin/sh
set -eu
B64=$(base64 -w0 /tmp/photo.jpg)
cat > /tmp/body.json <<JSON
{"images":["$B64"],"hint":""}
JSON
echo "=== /api/v1/scan-v2 (Gemini 2.5 Pro) ==="
curl -s -X POST http://localhost:3000/api/v1/scan-v2 \
  -H "Content-Type: application/json" \
  -H "x-api-key: ${API_KEY}" \
  --data-binary @/tmp/body.json \
  -w "\nHTTP %{http_code} in %{time_total}s\n"
EOF

scp -q "$TMP_JPG" vps:/tmp/photo.jpg
scp -q "$SCRIPT" vps:/tmp/run.sh

ssh vps "
  docker cp /tmp/photo.jpg nutrition-api-nutrition-api-1:/tmp/photo.jpg >/dev/null
  docker cp /tmp/run.sh nutrition-api-nutrition-api-1:/tmp/run.sh >/dev/null
  docker compose -f ~/nutrition-api/docker-compose.yml exec -T nutrition-api sh /tmp/run.sh
"

# Pretty print the JSON response
echo ""
echo "=== Pretty totals ==="
ssh vps "docker logs nutrition-api-nutrition-api-1 --tail 5 2>&1" | grep "scan-v2" | tail -2

rm -f "$TMP_JPG" "$SCRIPT"
