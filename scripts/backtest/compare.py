#!/usr/bin/env python3
"""
Compare two backtest runs side-by-side.

Usage:
  python3 compare.py runs/<run-A> runs/<run-B>
  python3 compare.py --latest 2          # compare last 2 runs

Prints a delta table: which macros got better/worse between two variants.
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

ROOT = Path(__file__).parent
RUNS_DIR = ROOT / "runs"


def latest_runs(n: int) -> list[Path]:
    runs = sorted([d for d in RUNS_DIR.iterdir() if d.is_dir()])
    if len(runs) < n:
        print(f"Need at least {n} runs, found {len(runs)}")
        sys.exit(1)
    return runs[-n:]


def load_run(folder: Path) -> dict:
    summary = json.loads((folder / "summary.json").read_text())
    rows = list(csv.DictReader((folder / "details.csv").open()))
    return {"summary": summary, "rows": rows, "folder": folder}


def fmt_delta(a: float, b: float) -> str:
    delta = b - a
    arrow = "✅" if delta < 0 else ("❌" if delta > 0 else "=")
    return f"{delta:+.1f}% {arrow}"


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("a", nargs="?", help="First run folder")
    p.add_argument("b", nargs="?", help="Second run folder")
    p.add_argument("--latest", type=int, help="Compare the latest N runs")
    args = p.parse_args()

    if args.latest:
        runs = latest_runs(args.latest)
        a = load_run(runs[-2])
        b = load_run(runs[-1])
    elif args.a and args.b:
        a = load_run(Path(args.a))
        b = load_run(Path(args.b))
    else:
        p.error("Provide two run folders or --latest 2")

    sa = a["summary"]
    sb = b["summary"]
    va = sa["variant"]
    vb = sb["variant"]

    print(f"\nA: {a['folder'].name}  ({va['id']} on {va['model']})")
    print(f"   {va['description']}\n")
    print(f"B: {b['folder'].name}  ({vb['id']} on {vb['model']})")
    print(f"   {vb['description']}\n")

    print(f"{'macro':<10} {'A MAPE':>8} {'B MAPE':>8} {'Δ MAPE':>14} "
          f"{'A mean':>8} {'B mean':>8} {'Δ mean':>14}")
    for macro in ("kcal", "protein", "fat", "carbs"):
        ma = sa["summary"]["macros"].get(macro)
        mb = sb["summary"]["macros"].get(macro)
        if not ma or not mb:
            continue
        print(
            f"{macro:<10} {ma['mape']:>7.1f}% {mb['mape']:>7.1f}% "
            f"{fmt_delta(ma['mape'], mb['mape']):>14} "
            f"{ma['mean_err']:>+7.1f}% {mb['mean_err']:>+7.1f}% "
            f"{fmt_delta(ma['mean_err'], mb['mean_err']):>14}"
        )

    print()

    # Per-recipe regressions / improvements
    a_rows = {r["slug"]: r for r in a["rows"] if r["status"] == "ok"}
    b_rows = {r["slug"]: r for r in b["rows"] if r["status"] == "ok"}
    common = set(a_rows) & set(b_rows)

    biggest_improve = []
    biggest_regress = []
    for slug in common:
        ra = a_rows[slug]
        rb = b_rows[slug]
        try:
            a_avg = sum(abs(float(ra[f"err_{k}"])) for k in ("kcal", "protein", "fat", "carbs")) / 4
            b_avg = sum(abs(float(rb[f"err_{k}"])) for k in ("kcal", "protein", "fat", "carbs")) / 4
        except (ValueError, TypeError):
            continue
        delta = b_avg - a_avg
        biggest_improve.append((delta, slug, a_avg, b_avg))
    biggest_improve.sort()

    print("Top 5 improvements (A → B):")
    for delta, slug, a_avg, b_avg in biggest_improve[:5]:
        print(f"  {slug[:60]:<60}  {a_avg:5.1f}% → {b_avg:5.1f}%  ({delta:+.1f}%)")

    print("\nTop 5 regressions (A → B):")
    for delta, slug, a_avg, b_avg in reversed(biggest_improve[-5:]):
        print(f"  {slug[:60]:<60}  {a_avg:5.1f}% → {b_avg:5.1f}%  ({delta:+.1f}%)")

    return 0


if __name__ == "__main__":
    sys.exit(main())
