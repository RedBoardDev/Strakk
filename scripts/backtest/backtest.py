#!/usr/bin/env python3
"""
Backtest runner — measures macro accuracy of a prompt variant against the
50 scraped HelloFresh recipes.

A "variant" is a JSON file (see variants/v1-baseline.json) that specifies the
model, system prompt, and parameters to test. The runner sends each recipe's
photo to Gemini using that variant, compares the predicted totals to the
recipe's known per-serving macros, and saves:

  runs/<timestamp>-<variant-id>/
    summary.json   — aggregate MAPE / mean error / stdev per macro
    details.csv    — one row per recipe with real vs predicted vs error

Usage:
  python3 backtest.py variants/v1-baseline.json [--limit 5] [--workers 5]

Requires: GEMINI_API_KEY env var (locally), Pillow (image resize).
"""

from __future__ import annotations

import argparse
import base64
import csv
import io
import json
import os
import statistics
import sys
import threading
import time
from collections import deque
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


class RateLimiter:
    """Sliding-window rate limiter. Blocks acquire() until a request
    can be made without exceeding `rpm` requests in the last 60 seconds."""

    def __init__(self, rpm: int):
        self.rpm = rpm
        self.window = 60.0
        self.timestamps: deque[float] = deque()
        self.lock = threading.Lock()

    def acquire(self) -> None:
        while True:
            with self.lock:
                now = time.time()
                while self.timestamps and self.timestamps[0] < now - self.window:
                    self.timestamps.popleft()
                if len(self.timestamps) < self.rpm:
                    self.timestamps.append(now)
                    return
                wait = self.window - (now - self.timestamps[0]) + 0.05
            time.sleep(min(wait, 1.0))


# Module-level singleton, set by main(). None disables limiting.
_RATE_LIMITER: RateLimiter | None = None


def set_rate_limiter(rpm: int | None) -> None:
    global _RATE_LIMITER
    _RATE_LIMITER = RateLimiter(rpm) if rpm else None

ROOT = Path(__file__).parent
RECIPES_DIR = ROOT / "recipes"
RUNS_DIR = ROOT / "runs"
GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"

# Schema we ask Gemini to fill — kept in sync with the production adapter.
RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "items": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "unit": {"type": "string", "enum": ["g", "ml"]},
                    "amount": {"type": "number"},
                    "kcal": {"type": "number"},
                    "protein_g": {"type": "number"},
                    "fat_g": {"type": "number"},
                    "carbs_g": {"type": "number"},
                },
                "required": ["name", "unit", "amount", "kcal", "protein_g", "fat_g", "carbs_g"],
            },
        }
    },
    "required": ["items"],
}


@dataclass
class Variant:
    id: str
    description: str
    model: str
    system_prompt: str
    temperature: float = 0.2

    @classmethod
    def load(cls, path: Path) -> "Variant":
        d = json.loads(path.read_text())
        return cls(
            id=d["id"],
            description=d.get("description", ""),
            model=d.get("model", "gemini-2.5-pro"),
            system_prompt=d["system_prompt"],
            temperature=d.get("temperature", 0.2),
        )


@dataclass
class Recipe:
    slug: str
    photo_path: Path
    real: dict  # {kcal, protein, fat, carbs}
    name: str

    @property
    def has_real_values(self) -> bool:
        return all(self.real.get(k) for k in ("kcal", "protein", "fat", "carbs"))


def load_recipes() -> list[Recipe]:
    items: list[Recipe] = []
    for d in sorted(RECIPES_DIR.iterdir()):
        if not d.is_dir():
            continue
        data_path = d / "data.json"
        photo_path = d / "photo.jpg"
        if not data_path.exists() or not photo_path.exists():
            continue
        data = json.loads(data_path.read_text())
        n = data.get("nutrition_per_serving") or {}
        items.append(
            Recipe(
                slug=d.name,
                photo_path=photo_path,
                name=data.get("name", d.name),
                real={
                    "kcal": n.get("kcal"),
                    "protein": n.get("protein_g"),
                    "fat": n.get("fat_g"),
                    "carbs": n.get("carbs_g"),
                },
            )
        )
    return items


def resize_image(src: Path, max_side: int = 1024, quality: int = 80) -> bytes:
    """Resize and re-encode as JPEG to keep payload small."""
    from PIL import Image  # local import: keep optional dep contained

    with Image.open(src) as img:
        img = img.convert("RGB")
        img.thumbnail((max_side, max_side), Image.LANCZOS)
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=quality, optimize=True)
        return buf.getvalue()


def call_gemini(api_key: str, variant: Variant, jpeg_bytes: bytes) -> dict:
    """Fire one Gemini request. Returns parsed { items: [...] } or raises."""
    if _RATE_LIMITER is not None:
        _RATE_LIMITER.acquire()
    body = {
        "systemInstruction": {"parts": [{"text": variant.system_prompt}]},
        "contents": [{
            "role": "user",
            "parts": [{
                "inline_data": {
                    "mime_type": "image/jpeg",
                    "data": base64.b64encode(jpeg_bytes).decode("ascii"),
                }
            }],
        }],
        "generationConfig": {
            "temperature": variant.temperature,
            "responseMimeType": "application/json",
            "responseSchema": RESPONSE_SCHEMA,
        },
    }
    url = GEMINI_ENDPOINT.format(model=variant.model) + f"?key={api_key}"
    req = Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urlopen(req, timeout=120) as r:
        payload = json.loads(r.read())
    text = payload["candidates"][0]["content"]["parts"][0]["text"]
    return json.loads(text)


def aggregate_totals(items: list[dict]) -> dict:
    totals = {"kcal": 0.0, "protein": 0.0, "fat": 0.0, "carbs": 0.0}
    for it in items:
        totals["kcal"] += float(it.get("kcal") or 0)
        totals["protein"] += float(it.get("protein_g") or 0)
        totals["fat"] += float(it.get("fat_g") or 0)
        totals["carbs"] += float(it.get("carbs_g") or 0)
    return totals


def pct_error(predicted: float, real: float) -> float | None:
    if not real:
        return None
    return (predicted - real) / real * 100.0


def run_one(api_key: str, variant: Variant, recipe: Recipe) -> dict:
    """Process a single recipe. Returns row dict (success or failure)."""
    row = {
        "slug": recipe.slug,
        "name": recipe.name,
        "real_kcal": recipe.real["kcal"],
        "real_protein": recipe.real["protein"],
        "real_fat": recipe.real["fat"],
        "real_carbs": recipe.real["carbs"],
        "pred_kcal": None,
        "pred_protein": None,
        "pred_fat": None,
        "pred_carbs": None,
        "err_kcal": None,
        "err_protein": None,
        "err_fat": None,
        "err_carbs": None,
        "items_summary": "",
        "latency_s": None,
        "status": "pending",
        "error": "",
    }

    if not recipe.has_real_values:
        row["status"] = "skipped"
        row["error"] = "missing real values"
        return row

    try:
        jpeg = resize_image(recipe.photo_path)
    except Exception as e:
        row["status"] = "error"
        row["error"] = f"resize: {e}"
        return row

    start = time.time()
    for attempt in range(3):
        try:
            result = call_gemini(api_key, variant, jpeg)
            break
        except HTTPError as e:
            if e.code in (429, 500, 503) and attempt < 2:
                # Exponential backoff for rate limits and server errors.
                time.sleep(2 ** attempt * 5)
                continue
            row["status"] = "error"
            row["error"] = f"HTTP {e.code}: {e.read().decode('utf-8', errors='ignore')[:200]}"
            return row
        except URLError as e:
            if attempt < 2:
                time.sleep(2)
                continue
            row["status"] = "error"
            row["error"] = f"URL: {e}"
            return row
        except Exception as e:  # broad — we want to capture model parse errors too
            row["status"] = "error"
            row["error"] = f"{type(e).__name__}: {e}"
            return row
    else:
        row["status"] = "error"
        row["error"] = "max retries exhausted"
        return row

    row["latency_s"] = round(time.time() - start, 2)

    items = result.get("items") or []
    totals = aggregate_totals(items)
    row["pred_kcal"] = round(totals["kcal"], 1)
    row["pred_protein"] = round(totals["protein"], 1)
    row["pred_fat"] = round(totals["fat"], 1)
    row["pred_carbs"] = round(totals["carbs"], 1)
    row["err_kcal"] = pct_error(totals["kcal"], recipe.real["kcal"])
    row["err_protein"] = pct_error(totals["protein"], recipe.real["protein"])
    row["err_fat"] = pct_error(totals["fat"], recipe.real["fat"])
    row["err_carbs"] = pct_error(totals["carbs"], recipe.real["carbs"])
    row["items_summary"] = " | ".join(
        f"{i.get('name')} {i.get('amount')}{i.get('unit')}" for i in items
    )
    row["status"] = "ok"
    return row


def summarize(rows: list[dict]) -> dict:
    ok = [r for r in rows if r["status"] == "ok"]
    failed = [r for r in rows if r["status"] == "error"]
    skipped = [r for r in rows if r["status"] == "skipped"]

    summary: dict = {
        "n_ok": len(ok),
        "n_failed": len(failed),
        "n_skipped": len(skipped),
        "macros": {},
    }

    for macro in ("kcal", "protein", "fat", "carbs"):
        errs = [r[f"err_{macro}"] for r in ok if r[f"err_{macro}"] is not None]
        if not errs:
            continue
        abs_errs = [abs(e) for e in errs]
        summary["macros"][macro] = {
            "mape": round(statistics.mean(abs_errs), 2),
            "mean_err": round(statistics.mean(errs), 2),
            "median_err": round(statistics.median(errs), 2),
            "stdev": round(statistics.stdev(errs) if len(errs) > 1 else 0, 2),
            "n": len(errs),
            "within_5pct": sum(1 for e in abs_errs if e <= 5),
            "within_10pct": sum(1 for e in abs_errs if e <= 10),
            "within_20pct": sum(1 for e in abs_errs if e <= 20),
        }
    return summary


def print_summary(variant: Variant, summary: dict) -> None:
    print(f"\n=== {variant.id} — {variant.description} ===")
    print(f"Model: {variant.model}")
    print(
        f"Recipes: {summary['n_ok']} ok, {summary['n_failed']} failed, "
        f"{summary['n_skipped']} skipped"
    )
    print()
    print(
        f"{'macro':<10} {'MAPE':>7} {'mean':>7} {'median':>8} {'stdev':>7} "
        f"{'≤5%':>5} {'≤10%':>5} {'≤20%':>5}"
    )
    for macro, m in summary["macros"].items():
        print(
            f"{macro:<10} {m['mape']:>6.1f}% {m['mean_err']:>+6.1f}% "
            f"{m['median_err']:>+7.1f}% {m['stdev']:>6.1f} "
            f"{m['within_5pct']:>5} {m['within_10pct']:>5} {m['within_20pct']:>5}"
        )
    print()


def save_run(variant: Variant, rows: list[dict], summary: dict) -> Path:
    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    folder = RUNS_DIR / f"{ts}-{variant.id}"
    folder.mkdir(parents=True, exist_ok=True)

    (folder / "summary.json").write_text(
        json.dumps(
            {
                "variant": {
                    "id": variant.id,
                    "description": variant.description,
                    "model": variant.model,
                    "temperature": variant.temperature,
                    "system_prompt": variant.system_prompt,
                },
                "summary": summary,
                "timestamp": ts,
            },
            ensure_ascii=False,
            indent=2,
        )
    )

    csv_path = folder / "details.csv"
    fieldnames = [
        "slug",
        "name",
        "real_kcal",
        "real_protein",
        "real_fat",
        "real_carbs",
        "pred_kcal",
        "pred_protein",
        "pred_fat",
        "pred_carbs",
        "err_kcal",
        "err_protein",
        "err_fat",
        "err_carbs",
        "latency_s",
        "status",
        "error",
        "items_summary",
    ]
    with csv_path.open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for r in rows:
            row = {k: r.get(k) for k in fieldnames}
            for k in ("err_kcal", "err_protein", "err_fat", "err_carbs"):
                if row[k] is not None:
                    row[k] = round(row[k], 1)
            writer.writerow(row)

    return folder


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("variant", help="Path to variant JSON")
    p.add_argument("--limit", type=int, default=None, help="Limit recipes processed")
    p.add_argument(
        "--rpm",
        type=int,
        default=100,
        help="Max requests per minute (Gemini 2.5 Pro tier 1: 150 RPM, default 100 to leave headroom)",
    )
    p.add_argument(
        "--workers",
        type=int,
        default=20,
        help="Parallel HTTP workers — high is fine because RPM limiter regulates submission",
    )
    args = p.parse_args()

    set_rate_limiter(args.rpm)

    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("Set GEMINI_API_KEY env var.")
        return 1

    variant_path = Path(args.variant)
    if not variant_path.exists():
        print(f"Variant not found: {variant_path}")
        return 1

    variant = Variant.load(variant_path)
    recipes = load_recipes()
    if args.limit:
        recipes = recipes[: args.limit]

    print(
        f"Backtesting variant '{variant.id}' on {len(recipes)} recipes "
        f"({args.workers} workers, capped at {args.rpm} RPM)…"
    )

    rows: list[dict] = []
    start = time.time()
    with ThreadPoolExecutor(max_workers=args.workers) as ex:
        futures = {ex.submit(run_one, api_key, variant, r): r for r in recipes}
        for i, fut in enumerate(as_completed(futures), 1):
            row = fut.result()
            rows.append(row)
            if row["status"] == "ok":
                k = row["err_kcal"]
                p_ = row["err_protein"]
                f_ = row["err_fat"]
                c = row["err_carbs"]
                print(
                    f"[{i:3d}/{len(recipes)}] {row['slug'][:50]:<50} "
                    f"kcal {k:+5.1f}%  prot {p_:+5.1f}%  fat {f_:+5.1f}%  carbs {c:+5.1f}%"
                )
            else:
                print(
                    f"[{i:3d}/{len(recipes)}] {row['slug'][:50]:<50} "
                    f"{row['status']}: {row['error'][:80]}"
                )

    duration = time.time() - start
    print(f"\nFinished in {duration:.1f}s")

    summary = summarize(rows)
    print_summary(variant, summary)

    folder = save_run(variant, rows, summary)
    print(f"Saved to {folder}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
