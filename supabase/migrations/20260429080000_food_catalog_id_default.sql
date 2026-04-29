-- Auto-allocate id from food_catalog_off_id_seq when caller doesn't provide one.
-- CIQUAL inserts continue to set id explicitly (their alim_code is the PK), so
-- the default kicks in only for OFF live upserts.
ALTER TABLE food_catalog
    ALTER COLUMN id SET DEFAULT nextval('food_catalog_off_id_seq');

-- The Edge Function no longer needs the helper batch-id allocator; drop it.
DROP FUNCTION IF EXISTS nextvals_off_seq(int);
