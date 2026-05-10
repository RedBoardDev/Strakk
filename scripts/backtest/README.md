# Meal-scan Backtest Framework

Iterate on prompts/models for the photo meal-scan feature against 50 real
HelloFresh recipes (with verified macros).

## One-time setup

```bash
# Python deps (just Pillow for image resize)
pip3 install pillow

# Make sure your Gemini API key is exported
export GEMINI_API_KEY=AIza...

# (only the first time, to refresh the recipe corpus)
python3 scrape-hellofresh.py 50
```

## Running a backtest

Each prompt/model combination is a "variant" stored in `variants/<id>.json`.

```bash
# Run the latest variant on all 50 recipes (5 parallel workers, ~3-4 min)
python3 backtest.py variants/v1-baseline.json

# Smaller test on 5 recipes
python3 backtest.py variants/v1-baseline.json --limit 5

# Tweak parallelism (Gemini 2.5 Pro paid: ~6 RPS safe)
python3 backtest.py variants/v1-baseline.json --workers 8
```

Results are written to `runs/<timestamp>-<variant-id>/`:
- `summary.json` — variant config + aggregate MAPE/mean/stdev per macro
- `details.csv` — one row per recipe with real, predicted, error %

The console prints the same summary plus per-recipe progress.

## Comparing two runs

```bash
# Compare the last two runs you executed
python3 compare.py --latest 2

# Or specify explicitly
python3 compare.py runs/20260508-150000-v1-baseline runs/20260508-160000-v2-foo
```

Output:
- Δ MAPE per macro (✅ better, ❌ worse)
- Top 5 improved recipes / Top 5 regressed recipes

## Iterating on a new variant

1. Copy an existing variant: `cp variants/v1-baseline.json variants/v2-myidea.json`
2. Edit the `id`, `description`, and `system_prompt`
3. Run the backtest: `python3 backtest.py variants/v2-myidea.json`
4. Compare: `python3 compare.py --latest 2`
5. If it improved, keep iterating. If it regressed, look at the per-recipe details.

## Variant JSON schema

```json
{
  "id": "v2-conservative-fat",
  "description": "Reduces fat over-estimation by anchoring on lean cuts.",
  "model": "gemini-2.5-pro",
  "temperature": 0.2,
  "system_prompt": "..."
}
```

Supported `model`: any Gemini model that accepts vision input (`gemini-2.5-pro`,
`gemini-2.5-flash`, etc.).

## Useful metrics

- **MAPE** (Mean Absolute Percentage Error): the average |error|% across all 50
  recipes. Lower is better. This is the headline number.
- **Mean error** (signed): bias direction. Positive = over-estimating, negative
  = under-estimating.
- **stdev**: how variable the model is. Low stdev + low MAPE = consistent.
- **≤5% / ≤10% / ≤20%**: how many recipes fall within each error band. Useful
  to know e.g. "20/50 recipes are within 10% on protein".

## Cost & latency

- Gemini 2.5 Pro paid: ~$0.005 per recipe → **$0.25 per full backtest**
- 50 recipes × ~10s API call / 5 workers = **~100 seconds wall-clock**
- Free tier (5 RPM) is too tight for this — billing must be enabled

## Roadmap of variants to try

- `v1-baseline.json` ✅ — current production prompt
- `v2-show-reasoning.json` — ask Gemini to explain its choices
- `v3-fewer-instructions.json` — radically minimal prompt (let model think)
- `v4-flash-vs-pro.json` — same prompt, Flash vs Pro
- `v5-with-recipe-name.json` — pass the dish name as `hint` (cheating-ish but
  shows the upper bound when user provides context)
- `v6-multi-pass-validation.json` — second pass to critique first
