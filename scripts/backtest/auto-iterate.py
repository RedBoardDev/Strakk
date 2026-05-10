#!/usr/bin/env python3
"""
Auto-iterating prompt optimizer for the meal-scan pipeline.

Loop:
  1. Run backtest on the current best variant.
  2. Send aggregate metrics + worst-case examples to Claude as the "optimizer".
  3. Optimizer proposes a refined system prompt.
  4. Run backtest on the new variant.
  5. Keep the variant with the lowest combined MAPE; iterate from there.

Stops after N iterations OR when no improvement for K consecutive rounds.

Requires:
  GEMINI_API_KEY    — for the meal-scan model under test
  ANTHROPIC_API_KEY — for the optimizer (Claude Sonnet 4.6)

Usage:
  python3 auto-iterate.py --start variants/v1-baseline.json --max-rounds 10
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

ROOT = Path(__file__).parent
VARIANTS_DIR = ROOT / "variants"
RUNS_DIR = ROOT / "runs"
EVOLUTION_LOG = ROOT / "evolution.jsonl"

# Backtest helpers reused as a library.
sys.path.insert(0, str(ROOT))
from backtest import (
    Variant,
    load_recipes,
    run_one,
    summarize,
    save_run,
    print_summary,
    set_rate_limiter,
)
from concurrent.futures import ThreadPoolExecutor, as_completed

OPTIMIZER_MODEL = "claude-sonnet-4-6"
OPTIMIZER_ENDPOINT = "https://api.anthropic.com/v1/messages"


def combined_mape(summary: dict) -> float:
    """Weighted combined MAPE across the 4 macros — the headline number to minimize."""
    macros = summary["summary"]["macros"]
    weights = {"kcal": 1.0, "protein": 1.0, "fat": 1.0, "carbs": 1.0}
    total = 0.0
    n = 0
    for k, w in weights.items():
        if k in macros:
            total += macros[k]["mape"] * w
            n += w
    return total / n if n else float("inf")


def run_backtest(api_key: str, variant: Variant, recipes, workers: int = 5) -> tuple[list[dict], dict]:
    rows: list[dict] = []
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {ex.submit(run_one, api_key, variant, r): r for r in recipes}
        for i, fut in enumerate(as_completed(futures), 1):
            row = fut.result()
            rows.append(row)
            tag = "ok" if row["status"] == "ok" else row["status"]
            print(f"  [{i:3d}/{len(recipes)}] {row['slug'][:40]:<40}  {tag}", flush=True)
    summary = summarize(rows)
    return rows, summary


def pick_worst_examples(rows: list[dict], n: int = 5) -> list[dict]:
    """Return the n recipes with the highest combined absolute error across macros."""
    scored = []
    for r in rows:
        if r["status"] != "ok":
            continue
        try:
            score = sum(
                abs(float(r[f"err_{k}"])) for k in ("kcal", "protein", "fat", "carbs")
            ) / 4
        except (TypeError, ValueError):
            continue
        scored.append((score, r))
    scored.sort(key=lambda x: -x[0])
    return [r for _, r in scored[:n]]


def call_optimizer(api_key: str, current_prompt: str, summary: dict, worst: list[dict]) -> dict:
    """Ask Claude to propose a refined system prompt."""
    macros = summary["summary"]["macros"]

    metrics_block = "\n".join(
        f"- {k}: MAPE {m['mape']:.1f}%, mean error {m['mean_err']:+.1f}%, "
        f"median {m['median_err']:+.1f}%, stdev {m['stdev']:.1f}, "
        f"{m['within_10pct']}/{m['n']} within ±10%"
        for k, m in macros.items()
    )

    examples = []
    for r in worst:
        examples.append(
            f"Recipe: {r['name']}\n"
            f"  Real:      kcal {r['real_kcal']}, protein {r['real_protein']}g, "
            f"fat {r['real_fat']}g, carbs {r['real_carbs']}g\n"
            f"  Predicted: kcal {r['pred_kcal']}, protein {r['pred_protein']}g, "
            f"fat {r['pred_fat']}g, carbs {r['pred_carbs']}g\n"
            f"  Error:     kcal {r['err_kcal']:+.1f}%, protein {r['err_protein']:+.1f}%, "
            f"fat {r['err_fat']:+.1f}%, carbs {r['err_carbs']:+.1f}%\n"
            f"  Items: {r['items_summary'][:300]}"
        )
    examples_block = "\n\n".join(examples)

    user_msg = f"""You are improving the system prompt for a meal-scan AI. The AI gets a single photo of a plated meal and must output the kcal/protein/fat/carbs of each visible item. We backtest its accuracy against 50 real HelloFresh recipes whose nutrition labels we know.

CURRENT SYSTEM PROMPT
=====================
{current_prompt}

BACKTEST METRICS (50 recipes, lower MAPE = better)
==================================================
{metrics_block}

5 WORST RECIPES (largest combined error, the model fails on these the most)
==========================================================================
{examples_block}

YOUR JOB
========
Propose ONE refined system prompt that should reduce the dominant error pattern. Pick the macro with the worst MAPE, identify the systematic bias from the worst-case examples, and surgically improve the prompt to address it without introducing new biases or hardcoded lists.

Constraints:
- Keep the response schema the same (items array with name, unit, amount, kcal, protein_g, fat_g, carbs_g)
- Do NOT hardcode food names, recipes, or numerical reference values (no "fork = 19 cm", no "duck = 120 g") — those biases hurt as much as they help.
- Keep the prompt concise (≤500 words). Concise prompts perform better than long ones.
- The change should be principled, not a list of one-off rules.

Output JSON only:
{{
  "analysis": "1-2 sentences on the dominant error pattern",
  "hypothesis": "1 sentence on why your edit will help",
  "new_prompt": "the full new system prompt"
}}"""

    body = {
        "model": OPTIMIZER_MODEL,
        "max_tokens": 4096,
        "temperature": 0.4,
        "messages": [{"role": "user", "content": user_msg}],
    }
    req = Request(
        OPTIMIZER_ENDPOINT,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
        },
    )
    with urlopen(req, timeout=120) as r:
        payload = json.loads(r.read())

    text = payload["content"][0]["text"]
    # Strip markdown code fences if present.
    if "```json" in text:
        text = text.split("```json", 1)[1].split("```", 1)[0]
    elif "```" in text:
        text = text.split("```", 1)[1].split("```", 1)[0]
    return json.loads(text.strip())


def append_evolution(entry: dict) -> None:
    with EVOLUTION_LOG.open("a") as f:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--start", default="variants/v1-baseline.json", help="Starting variant")
    p.add_argument("--max-rounds", type=int, default=10, help="Max optimization rounds")
    p.add_argument("--patience", type=int, default=3, help="Stop after N rounds without improvement")
    p.add_argument("--rpm", type=int, default=100, help="Gemini requests/minute (tier 1: 150)")
    p.add_argument("--workers", type=int, default=20, help="HTTP workers (regulated by --rpm)")
    p.add_argument("--limit", type=int, default=None, help="Limit recipes (for fast iteration)")
    args = p.parse_args()

    set_rate_limiter(args.rpm)

    gem = os.environ.get("GEMINI_API_KEY")
    ant = os.environ.get("ANTHROPIC_API_KEY")
    if not gem:
        print("Set GEMINI_API_KEY"); return 1
    if not ant:
        print("Set ANTHROPIC_API_KEY"); return 1

    recipes = load_recipes()
    if args.limit:
        recipes = recipes[: args.limit]
    print(
        f"Optimizing on {len(recipes)} recipes, max {args.max_rounds} rounds, "
        f"patience {args.patience}, capped at {args.rpm} RPM with {args.workers} workers\n"
    )

    current_variant = Variant.load(Path(args.start))
    print(f"=== Round 0 — baseline ({current_variant.id}) ===")
    rows, summary = run_backtest(gem, current_variant, recipes, args.workers)
    save_run(current_variant, rows, summary)
    print_summary(current_variant, summary)
    best_score = combined_mape({"summary": summary})
    best_variant = current_variant
    best_rows = rows
    print(f"Combined MAPE: {best_score:.2f}%\n")
    append_evolution({
        "round": 0,
        "variant_id": current_variant.id,
        "combined_mape": best_score,
        "macros": summary["macros"],
        "kept": True,
    })

    rounds_without_improvement = 0
    for r in range(1, args.max_rounds + 1):
        if rounds_without_improvement >= args.patience:
            print(f"\nNo improvement for {args.patience} rounds — stopping.")
            break

        print(f"\n=== Round {r} — asking optimizer for a new variant ===")
        worst = pick_worst_examples(best_rows, n=5)
        try:
            proposal = call_optimizer(ant, best_variant.system_prompt, {"summary": summary}, worst)
        except HTTPError as e:
            print(f"Optimizer error: {e.code} {e.read()[:200]}")
            break
        except Exception as e:
            print(f"Optimizer error: {e}")
            break

        print(f"\nAnalysis: {proposal.get('analysis', '')}")
        print(f"Hypothesis: {proposal.get('hypothesis', '')}\n")

        ts = datetime.now().strftime("%Y%m%d-%H%M%S")
        new_id = f"auto-r{r}-{ts}"
        new_variant_path = VARIANTS_DIR / f"{new_id}.json"
        new_variant_data = {
            "id": new_id,
            "description": f"Auto round {r}: {proposal.get('hypothesis', '')[:200]}",
            "model": current_variant.model,
            "temperature": current_variant.temperature,
            "system_prompt": proposal["new_prompt"],
            "parent_id": best_variant.id,
            "analysis": proposal.get("analysis"),
        }
        new_variant_path.write_text(json.dumps(new_variant_data, ensure_ascii=False, indent=2))
        new_variant = Variant.load(new_variant_path)

        print(f"=== Running backtest on {new_id} ===")
        new_rows, new_summary = run_backtest(gem, new_variant, recipes, args.workers)
        save_run(new_variant, new_rows, new_summary)
        print_summary(new_variant, new_summary)

        new_score = combined_mape({"summary": new_summary})
        improved = new_score < best_score
        delta = new_score - best_score
        marker = "✅ KEEP" if improved else "❌ REVERT"
        print(f"Combined MAPE: {new_score:.2f}% (Δ {delta:+.2f}%) {marker}\n")

        append_evolution({
            "round": r,
            "variant_id": new_id,
            "parent_id": best_variant.id,
            "combined_mape": new_score,
            "delta": delta,
            "macros": new_summary["macros"],
            "kept": improved,
            "analysis": proposal.get("analysis"),
            "hypothesis": proposal.get("hypothesis"),
        })

        if improved:
            best_variant = new_variant
            best_score = new_score
            best_rows = new_rows
            summary = new_summary
            rounds_without_improvement = 0
        else:
            rounds_without_improvement += 1

    print("\n=== FINAL BEST VARIANT ===")
    print(f"id: {best_variant.id}")
    print(f"combined MAPE: {best_score:.2f}%")
    print(f"variant file: variants/{best_variant.id}.json")
    print(f"\nEvolution log: {EVOLUTION_LOG}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
