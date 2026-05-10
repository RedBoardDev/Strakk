#!/usr/bin/env python3
"""
Scrape ~50 HelloFresh.fr recipes for backtesting the meal-scan pipeline.

For each recipe we save:
  recipes/<slug>/
    data.json   — name, ingredients, nutrition (per serving), source URL
    photo.jpg   — main hero image

Run: python3 scrape-hellofresh.py [N=50]
"""

from __future__ import annotations

import json
import os
import re
import sys
import time
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).parent
RECIPES_DIR = ROOT / "recipes"
HOME_URL = "https://www.hellofresh.fr/recipes/"
USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
MAX_RECIPES = int(sys.argv[1]) if len(sys.argv) > 1 else 50
DELAY_SEC = 0.5  # be polite, avoid hammering


def fetch(url: str) -> bytes:
    req = Request(url, headers={"User-Agent": USER_AGENT, "Accept-Language": "fr-FR"})
    with urlopen(req, timeout=30) as r:
        return r.read()


CATEGORY_PAGES = [
    "",  # home
    "american-recipes",
    "asian-recipes",
    "chinese-recipes",
    "comfort-recipes",
    "easy-recipes",
    "japanese-recipes",
    "korean-recipes",
    "mediterranean-recipes",
    "mexican-recipes",
    "middle-eastern-recipes",
    "moroccan-recipes",
    "most-popular-recipes",
    "quick-recipes",
]


def list_recipe_urls() -> list[str]:
    """Crawl home + every category page, return unique recipe slugs."""
    seen: set[str] = set()
    unique: list[str] = []
    for slug in CATEGORY_PAGES:
        url = HOME_URL if slug == "" else f"{HOME_URL}{slug}"
        try:
            html = fetch(url).decode("utf-8", errors="replace")
        except Exception as e:
            print(f"  category {slug or 'home'}: fetch failed ({e})")
            continue
        matches = re.findall(r"/recipes/[a-z0-9-]+-[a-f0-9]{24}", html)
        before = len(unique)
        for m in matches:
            if m not in seen:
                seen.add(m)
                unique.append(m)
        added = len(unique) - before
        print(f"  {slug or 'home':<25} +{added:3d} unique  (total: {len(unique)})")
        time.sleep(DELAY_SEC)
    return unique


def parse_recipe(url: str) -> dict | None:
    """Extract the schema.org Recipe JSON-LD block from a recipe page."""
    try:
        html = fetch(url).decode("utf-8", errors="replace")
    except Exception as e:
        print(f"  fetch failed: {e}")
        return None

    blocks = re.findall(
        r"<script[^>]*type=\"application/ld\+json\"[^>]*>(.*?)</script>",
        html,
        re.DOTALL,
    )
    for raw in blocks:
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            continue
        if data.get("@type") == "Recipe":
            return data
    return None


def parse_macro(value: str | None) -> float | None:
    """Convert '14.5 g' or '668 kcal' to a float, returns None if missing/empty."""
    if not value:
        return None
    m = re.search(r"([\d.]+)", value)
    return float(m.group(1)) if m else None


def slug_from_url(url: str) -> str:
    return url.rsplit("/", 1)[-1]


def already_done(slug: str) -> bool:
    folder = RECIPES_DIR / slug
    return (folder / "data.json").exists() and (folder / "photo.jpg").exists()


def save_recipe(url: str, recipe: dict) -> bool:
    slug = slug_from_url(url)
    folder = RECIPES_DIR / slug
    folder.mkdir(parents=True, exist_ok=True)

    nutrition = recipe.get("nutrition") or {}
    data = {
        "url": f"https://www.hellofresh.fr{url}",
        "slug": slug,
        "name": recipe.get("name"),
        "description": recipe.get("description"),
        "cuisine": recipe.get("recipeCuisine"),
        "category": recipe.get("recipeCategory"),
        "yield_servings": recipe.get("recipeYield"),
        "total_time": recipe.get("totalTime"),
        "ingredients": recipe.get("recipeIngredient") or [],
        "image_url": recipe.get("image"),
        "nutrition_per_serving": {
            "kcal": parse_macro(nutrition.get("calories")),
            "protein_g": parse_macro(nutrition.get("proteinContent")),
            "fat_g": parse_macro(nutrition.get("fatContent")),
            "saturated_fat_g": parse_macro(nutrition.get("saturatedFatContent")),
            "carbs_g": parse_macro(nutrition.get("carbohydrateContent")),
            "sugar_g": parse_macro(nutrition.get("sugarContent")),
            "fiber_g": parse_macro(nutrition.get("fiberContent")),
            "sodium_g": parse_macro(nutrition.get("sodiumContent")),
            "serving_size_g": parse_macro(nutrition.get("servingSize")),
        },
    }

    image_url = recipe.get("image")
    if not image_url:
        print("  ⚠ no image URL")
        return False

    try:
        img_bytes = fetch(image_url)
    except Exception as e:
        print(f"  ⚠ image fetch failed: {e}")
        return False

    (folder / "photo.jpg").write_bytes(img_bytes)
    (folder / "data.json").write_text(json.dumps(data, ensure_ascii=False, indent=2))
    return True


def main() -> int:
    RECIPES_DIR.mkdir(parents=True, exist_ok=True)

    print(f"Fetching recipe listing from {HOME_URL}…")
    urls = list_recipe_urls()
    print(f"Found {len(urls)} unique recipe URLs.")

    if not urls:
        print("No recipes found — page structure may have changed.")
        return 1

    saved = 0
    skipped = 0
    failed = 0
    for url in urls:
        if saved >= MAX_RECIPES:
            break
        slug = slug_from_url(url)
        if already_done(slug):
            skipped += 1
            continue

        print(f"[{saved + 1}/{MAX_RECIPES}] {slug}")
        recipe = parse_recipe(f"https://www.hellofresh.fr{url}")
        if not recipe:
            print("  no recipe JSON-LD found")
            failed += 1
            continue

        if save_recipe(url, recipe):
            saved += 1
            time.sleep(DELAY_SEC)
        else:
            failed += 1

    print(f"\nDone. saved={saved}, skipped={skipped}, failed={failed}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
