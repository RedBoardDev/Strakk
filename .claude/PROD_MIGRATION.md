# Mise à niveau Production — Strakk

> Créé le 2026-05-10. Objectif : amener `iwzqukzocbazgnxtwwfx` (prod) au même niveau que `kcalhgcqjncymsutkdmq` (staging).
> **Règle absolue : aucune suppression ni modification destructive sur prod. Zéro perte de données.**

---

## Résumé de l'état actuel

| | Staging | Production |
|---|---|---|
| Ref | `kcalhgcqjncymsutkdmq` | `iwzqukzocbazgnxtwwfx` |
| Région | Ireland | Paris |
| Migrations appliquées | 33/34 | 9/34 |
| Dernière migration | `20260509114500` (locale seulement) | `20260429090000` |
| Edge Functions | 9 | 6 |
| Backup prod | `supabase/backup_prod_20260510_182903.sql` (37KB schema) | |
| Backup data | `supabase/backup_prod_data_20260510_182903.sql` (135KB) | |

---

## Phase 1 — Secrets manquants (⚠️ BLOQUANT)

Ces secrets sont présents sur staging mais **absents sur prod**. Les edge functions qui en dépendent échoueront.

| Secret | Staging | Production | Action |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | ✅ | ✅ (même digest) | — |
| `NUTRITION_API_KEY` | ✅ | ❌ | 🔴 À fournir |
| `NUTRITION_API_URL` | ✅ | ❌ | 🔴 À fournir |
| `OPENAI_API_KEY` | ✅ | ❌ | 🔴 À fournir |
| `REVENUECAT_WEBHOOK_SECRET` | ✅ | ❌ | 🔴 À fournir |
| `SUPABASE_*` (6 clés) | ✅ | ✅ (valeurs différentes = normal) | — |

### Config iOS (xcconfig)

| Fichier | Clé | État |
|---|---|---|
| `iosApp/Config/Production.xcconfig` | `STRAKK_REVENUECAT_API_KEY` | 🔴 Doit avoir la clé prod RevenueCat |
| `local.properties` | `REVENUECAT_API_KEY` | 🔴 Actuellement clé TEST (`test_ipZhCBYCkScXPZcsbdhGEeydleQ`) |

**Questions pour le user :**
- [ ] Quelle est la clé prod RevenueCat iOS ? (`STRAKK_REVENUECAT_API_KEY` dans Production.xcconfig)
- [ ] Quelle est la clé prod RevenueCat Android ? (`REVENUECAT_API_KEY` dans local.properties)
- [ ] `NUTRITION_API_KEY`, `NUTRITION_API_URL`, `OPENAI_API_KEY`, `REVENUECAT_WEBHOOK_SECRET` — mêmes valeurs que staging ou différentes ?

---

## Phase 2 — Migrations (25 à appliquer)

> Status : ⬜ TODO | 🔄 En cours | ✅ Appliqué | ❌ Erreur

| # | Migration | Risque | Status | Notes |
|---|---|---|---|---|
| 1 | `20260430000000_security_hardening` | MEDIUM | ⬜ | Drop hevy_api_key plaintext — sûr si 20260428000000 bien appliqué (✅ sur prod) |
| 2 | `20260430100000_optimize_search_rpc` | LOW | ⬜ | Remplacement de fonction seulement |
| 3 | `20260501000000_schema_improvements` | **MEDIUM** | ⬜ | ⚠️ CAST ENUM meal_entries.source + checkins.ai_summary_lang — vérifier données avant |
| 4 | `20260501100000_add_check_constraints` | LOW | ⬜ | Contraintes IS NULL OR sur nullable — safe |
| 5 | `20260501200000_onboarding_v2_profile_fields` | LOW | ⬜ | ADD COLUMN IF NOT EXISTS nullable — safe |
| 6 | `20260503000000_add_subscriptions` | LOW | ⬜ | Nouvelle table |
| 7 | `20260503100000_auto_trial_on_signup` | LOW | ⬜ | Trigger new users seulement |
| 8 | `20260503110000_allow_self_expire_trial` | LOW | ⬜ | RLS policy uniquement |
| 9 | `20260503120000_add_feature_limits_and_usage` | LOW | ⬜ | Nouvelles tables + seed |
| 10 | `20260505100000_add_pgvector_extension` | LOW | ⬜ | CREATE EXTENSION IF NOT EXISTS — idempotent |
| 11 | `20260505100100_food_catalog_add_vector_columns` | LOW | ⬜ | ADD COLUMN nullable |
| 12 | `20260505100200_vector_search_rpc` | LOW | ⬜ | Nouvelle RPC |
| 13 | `20260505100300_meal_entries_grounding_columns` | LOW | ⬜ | ADD COLUMN nullable + FK nullable |
| 14 | `20260505100400_cooking_retention_factors` | LOW | ⬜ | Nouvelle table + seed |
| 15 | `20260506090000_food_catalog_usda_source_variants` | LOW | ⬜ | Expand constraint source |
| 16 | `20260506100000_food_catalog_hnsw_index` | MEDIUM | ⬜ | CREATE INDEX HNSW — lent si food_catalog volumineuse |
| 17 | `20260506100100_recreate_hnsw_index` | LOW | ⬜ | DROP + CREATE INDEX |
| 18 | `20260506100200_vector_search_timeout_and_analyze` | LOW | ⬜ | ANALYZE + RPC |
| 19 | `20260506100300_rebuild_hnsw_with_memory` | MEDIUM | ⬜ | Rebuild HNSW 512MB RAM — vérifier plan Supabase prod |
| 20 | `20260506100400_force_hnsw_index_usage` | LOW | ⬜ | RPC avec enable_seqscan=off |
| 21 | `20260506100500_deactivate_bad_foundation_items` | LOW | ⬜ | UPDATE is_active=false sur items calories=0 |
| 22 | `20260506110000_add_branded_source` | LOW | ⬜ | Expand constraint + UPDATE is_active=true |
| 23 | `20260506120000_drop_hnsw_for_bulk_import` | LOW | ⬜ | DROP INDEX seulement |
| 24 | `20260506130000_drop_vector_search_from_supabase` | **🔴 HIGH** | ⬜ | ⚠️ **DELETE tous USDA items** + DROP embedding — nécessite Qdrant sur prod |
| 25 | `20260509114500_harden_revenuecat_webhook` | LOW | ⬜ | Nouvelle table + contraintes NOT VALID (safe) |

---

## Phase 3 — Edge Functions (prod à mettre à jour)

| Fonction | Action | Status |
|---|---|---|
| `scan-meal` | 🆕 Déployer (remplace `analyze-meal-single` sur prod) | ⬜ |
| `calculate-goals` | 🆕 Déployer | ⬜ |
| `revenuecat-webhook` | 🆕 Déployer | ⬜ |
| `delete-account` | 🆕 Déployer | ⬜ |
| `extract-meal-draft` | 🔄 Mettre à jour | ⬜ |
| `search-off-live` | 🔄 Mettre à jour | ⬜ |
| `export-to-hevy` | 🔄 Mettre à jour | ⬜ |
| `parse-workout-pdf` | 🔄 Mettre à jour | ⬜ |
| `generate-checkin-summary` | 🔄 Mettre à jour | ⬜ |
| `analyze-meal-single` | ⚠️ À supprimer APRÈS déploiement de `scan-meal` | ⬜ |

> ⚠️ Ne pas supprimer `analyze-meal-single` tant que l'app prod l'utilise encore. Déployer `scan-meal` d'abord, vérifier, puis supprimer l'ancien.

---

## Phase 4 — Config app

| Fichier | Clé | Action | Status |
|---|---|---|---|
| `iosApp/Config/Production.xcconfig` | `STRAKK_REVENUECAT_API_KEY` | Mettre clé prod | ⬜ |
| `local.properties` | `REVENUECAT_API_KEY` | Remplacer clé test par clé prod | ⬜ |

---

## Phase 5 — Vérification finale

- [ ] App iOS (scheme Strakk prod) se connecte sans erreur
- [ ] Login / Auth fonctionne
- [ ] Onboarding v2 fonctionne
- [ ] Edge functions répondent
- [ ] RevenueCat webhook reçoit les events
- [ ] Données existantes intactes

---

## Questions en attente de réponse

1. **RevenueCat prod key iOS** (`STRAKK_REVENUECAT_API_KEY`) — quelle est la valeur ?
2. **RevenueCat prod key Android** (`REVENUECAT_API_KEY`) — même que iOS ou différente ?
3. **Secrets manquants sur prod** — `NUTRITION_API_KEY`, `NUTRITION_API_URL`, `OPENAI_API_KEY`, `REVENUECAT_WEBHOOK_SECRET` sont-ils les mêmes qu'en staging ou différents ?

---

## Log d'exécution

| Date | Action | Résultat |
|---|---|---|
| 2026-05-10 | Backup schema prod | ✅ `backup_prod_20260510_182903.sql` (37KB) |
| 2026-05-10 | Backup data prod | ✅ `backup_prod_data_20260510_182903.sql` (135KB) |
| 2026-05-10 | Analyse état prod vs staging | ✅ Voir ce fichier |
| 2026-05-10 | Pre-flight checks données prod | ✅ ENUM OK, food_catalog 0 USDA items |
| 2026-05-10 | Push 25 migrations prod | ✅ 34/34 Local=Remote |
| 2026-05-10 | Secrets prod mis à jour | ✅ 11 secrets présents (⚠️ REVENUECAT_WEBHOOK_SECRET = même digest que NUTRITION_API_KEY) |
| 2026-05-10 | Deploy 9 edge functions prod | ✅ calculate-goals, delete-account, export-to-hevy, extract-meal-draft, generate-checkin-summary, parse-workout-pdf, revenuecat-webhook, scan-meal, search-off-live |
| 2026-05-10 | analyze-meal-single conservée | ✅ Encore appelée par le code app (ProcessMealDraftUseCase, QuickAddFromPhotoUseCase) |
| 2026-05-10 | Re-link supabase → staging | ✅ |
