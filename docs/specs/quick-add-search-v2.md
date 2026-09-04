# Spec — Quick-Add Search Drawer V2 (My foods · Catalog · Favorites · Meals)

Status: in progress (2026-05-20)

## Why

Today's "Search a food" drawer mixes the user's own history with the catalog data
in a single scrolling list. There is no concept of favorites and no way to add a
**whole meal** (a previously-logged meal container) as a single tap — every
meal_entry has to be added one by one, which is noisy and slow.

The user reported three issues:

1. *"Search food mixes everything"* — no separation between **personal data**
   and the **shared catalog** (CIQUAL / OFF).
2. *"No way to favorite a food or a meal"* — no heart icon, no quick re-access.
3. *"You display per item, but adding a meal back should be one tap, not item per
   item — and I should be able to favorite the meal, not just items."*
4. *"History is not in chronological order."*

## Product decisions

### Top-level: tabbed drawer

The search drawer becomes two segmented tabs:

| Tab           | Purpose                                              |
| ------------- | ---------------------------------------------------- |
| **My foods**  | The user's own foods, favorites, and recent meals    |
| **Catalog**   | The shared CIQUAL / OFF catalog (current behavior)   |

### Tab 1 — My foods

#### Empty query

Two sections, in order:

1. **Favorites**
   - *Favorite meals* (filled-heart cards) — taps expand into "Add to today"
   - *Favorite foods* (rows) — taps expand into portion picker
2. **Recent** (chronological, most recent first)
   - *Recent meals*: distinct meals logged in the last 30 days (deduped by name,
     keeping the latest occurrence's items), sorted by `created_at DESC`
   - *Recent foods*: distinct food entries logged in the last 60 days, deduped
     by normalized name, sorted by `max(created_at) DESC`

#### Non-empty query

Filter the union of (favorites + recents) by name (normalized substring match).
Catalog is **not** searched from the My foods tab.

### Tab 2 — Catalog

Unchanged behavior — CIQUAL / OFF search via the existing
`FoodCatalogRepository.search()`.

### Adding a meal back

When the user taps a **meal** (either favorite or recent) in My foods, a single
"Add to today" button appears in the row. Tapping it creates a new `meals` row
+ all its `meal_entries` for today (deep clone, fresh ids, `created_at = now`).
No item-by-item picker — the meal is added as a single unit.

### Favorites

Two new tables (see Data model). The user can toggle a heart from:

- The search drawer (food row / meal card)
- The Today timeline (meal-container row / orphan-entry row)
- The Meal-detail sheet (whole-meal heart) and Entry-detail sheet (per-food)

Favorites **survive deletion** of the originating meal/entry — they are
denormalized templates, not references.

## Data model

```sql
-- favorite_foods: a denormalized food template (one row per favorite food)
CREATE TABLE favorite_foods (
    id              uuid             PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid             NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name            text             NOT NULL CHECK (char_length(name) BETWEEN 1 AND 100),
    name_normalized text             NOT NULL,
    protein         double precision NOT NULL DEFAULT 0,
    calories        double precision NOT NULL DEFAULT 0,
    fat             double precision,
    carbs           double precision,
    quantity        text,
    food_catalog_id bigint REFERENCES food_catalog(id) ON DELETE SET NULL,
    created_at      timestamptz      NOT NULL DEFAULT now(),
    UNIQUE (user_id, name_normalized)
);

-- favorite_meals: a denormalized meal template
CREATE TABLE favorite_meals (
    id              uuid             PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid             NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name            text             NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    items_json      jsonb            NOT NULL,
    -- The originating meal id, if any. Not an FK — favorite survives meal deletion.
    source_meal_id  uuid,
    created_at      timestamptz      NOT NULL DEFAULT now()
);
```

`items_json` is a JSONB array of:
```json
[{ "name": "...", "protein": 0, "calories": 0, "fat": 0, "carbs": 0, "quantity": "150g" }]
```

RLS: `USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid())`.

### Heart-state lookup

- Food in Today row → favorited iff
  `favorite_foods.name_normalized = normalize(entry.name)` for this user
- Meal in Today row → favorited iff
  `favorite_meals.source_meal_id = meal.id` for this user

## Shared module changes

### Domain

- New models: `FavoriteFood`, `FavoriteMeal`, `FavoriteMealItem`, `RecentMeal`,
  `RecentMealItem`
- New repository: `FavoritesRepository` (interface in `domain/repository/`)
- New use cases:
  - `ObserveFavoriteFoodsUseCase`
  - `ObserveFavoriteMealsUseCase`
  - `ObserveRecentMealsUseCase` (replaces partial use of meals on the today
    timeline for the search drawer scope)
  - `ToggleFavoriteFoodUseCase`
  - `ToggleFavoriteMealUseCase`
  - `AddMealFromTemplateUseCase` — clones a `FavoriteMeal` or `RecentMeal` as a
    new committed `Meal` for today
  - `IsFoodFavoritedUseCase` (or expose via flow)

### Presentation

- `SearchFoodViewModel` is refactored to a tabbed state:
  ```kotlin
  sealed interface SearchFoodUiState {
      data object Loading
      data class Ready(
          val selectedTab: SearchTab,
          val query: String,
          val myFoods: MyFoodsData,
          val catalog: CatalogData,
          val isSearching: Boolean,
      )
      data class Error(...)
  }
  enum class SearchTab { MyFoods, Catalog }
  data class MyFoodsData(
      val favoriteMeals: List<FavoriteMeal>,
      val favoriteFoods: List<FavoriteFood>,
      val recentMeals: List<RecentMeal>,
      val recentFoods: List<FrequentItem>,
  )
  data class CatalogData(val items: List<FoodCatalogItem>)
  ```
- New events: `SwitchTab(SearchTab)`, `ToggleFavoriteFood(FavoriteFoodInput)`,
  `ToggleFavoriteMeal(MealTemplateInput)`, `AddMealTemplate(MealTemplateInput)`
- `TodayViewModel` gains: `OnToggleFavoriteFood(entry)`,
  `OnToggleFavoriteMeal(meal)` and a `favoritesIndex` field for heart state.

### Data

- DTOs: `FavoriteFoodDto`, `FavoriteMealDto`, `FavoriteMealItemDto`
- Mappers
- `FavoritesRepositoryImpl` with Supabase calls + a per-user reactive cache
  (`MutableStateFlow<List<FavoriteFood>>`, same pattern as `NutritionRepositoryImpl`)
- New SQL helpers for recents (PostgREST or RPC for grouping/dedup)

## iOS UI

- Rebuild `SearchFoodView.swift` around a `Picker(selection:)` segmented control
  at the top.
- Add new sub-views: `FavoriteFoodRow`, `FavoriteMealCard`, `RecentMealRow`.
- Heart button on every row + on Today meal/entry rows.
- `MealDetailSheet`: heart in the header for the whole meal.
- `EntryDetailSheet`: heart for the food.

## Android UI

Parity: tabs via `SecondaryTabRow` or `SegmentedButton`, heart icons, same row
composables. Detekt-clean (LongMethod, MagicNumber).

## Out of scope

- Backfilling a heart state from arbitrary historical entries automatically
- Sharing favorites across users
- Editing a favorite template's macros directly
