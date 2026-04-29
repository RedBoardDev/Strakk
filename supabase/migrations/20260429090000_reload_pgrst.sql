-- Force PostgREST to reload its schema cache after the food_catalog
-- column additions. Without this, upserts may reject the new columns
-- with "could not find ... column" errors.
NOTIFY pgrst, 'reload schema';
