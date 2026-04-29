-- ================================================================
-- Strakk — Weekly Check-in schema
-- ================================================================
-- Tables: checkins, checkin_photos
-- Storage: checkin-photos bucket
-- ================================================================

----------------------------------------------------------------
-- 1. checkins
----------------------------------------------------------------

CREATE TABLE checkins (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    week_label      text        NOT NULL CHECK (week_label ~ '^\d{4}-W\d{2}$'),
    covered_dates   text[]      NOT NULL CHECK (array_length(covered_dates, 1) >= 1),
    weight_kg       double precision CHECK (weight_kg IS NULL OR (weight_kg > 0 AND weight_kg <= 500)),
    shoulders_cm    double precision CHECK (shoulders_cm IS NULL OR (shoulders_cm > 0 AND shoulders_cm <= 300)),
    chest_cm        double precision CHECK (chest_cm IS NULL OR (chest_cm > 0 AND chest_cm <= 300)),
    arm_left_cm     double precision CHECK (arm_left_cm IS NULL OR (arm_left_cm > 0 AND arm_left_cm <= 200)),
    arm_right_cm    double precision CHECK (arm_right_cm IS NULL OR (arm_right_cm > 0 AND arm_right_cm <= 200)),
    waist_cm        double precision CHECK (waist_cm IS NULL OR (waist_cm > 0 AND waist_cm <= 300)),
    hips_cm         double precision CHECK (hips_cm IS NULL OR (hips_cm > 0 AND hips_cm <= 300)),
    thigh_left_cm   double precision CHECK (thigh_left_cm IS NULL OR (thigh_left_cm > 0 AND thigh_left_cm <= 200)),
    thigh_right_cm  double precision CHECK (thigh_right_cm IS NULL OR (thigh_right_cm > 0 AND thigh_right_cm <= 200)),
    feeling_tags    text[]      DEFAULT '{}',
    feeling_note    text        CHECK (feeling_note IS NULL OR char_length(feeling_note) <= 1000),
    avg_protein     double precision,
    avg_calories    double precision,
    avg_fat         double precision,
    avg_carbs       double precision,
    avg_water       integer,
    nutrition_days  integer,
    ai_summary      text,
    ai_summary_lang text        DEFAULT 'fr' CHECK (ai_summary_lang IN ('fr', 'en')),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT unique_user_week UNIQUE (user_id, week_label)
);

CREATE INDEX idx_checkins_user_week ON checkins(user_id, week_label DESC);

ALTER TABLE checkins ENABLE ROW LEVEL SECURITY;

CREATE POLICY "checkins_owner_all" ON checkins
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

CREATE TRIGGER checkins_updated_at
    BEFORE UPDATE ON checkins
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

----------------------------------------------------------------
-- 2. checkin_photos
----------------------------------------------------------------

CREATE TABLE checkin_photos (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    checkin_id   uuid        NOT NULL REFERENCES checkins(id) ON DELETE CASCADE,
    storage_path text        NOT NULL,
    position     smallint    NOT NULL DEFAULT 0,
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_checkin_photos_checkin ON checkin_photos(checkin_id);

ALTER TABLE checkin_photos ENABLE ROW LEVEL SECURITY;

CREATE POLICY "checkin_photos_owner_all" ON checkin_photos
    USING (EXISTS (SELECT 1 FROM checkins c WHERE c.id = checkin_photos.checkin_id AND c.user_id = auth.uid()))
    WITH CHECK (EXISTS (SELECT 1 FROM checkins c WHERE c.id = checkin_photos.checkin_id AND c.user_id = auth.uid()));

----------------------------------------------------------------
-- 3. Storage bucket for check-in photos
----------------------------------------------------------------

INSERT INTO storage.buckets (id, name, public)
VALUES ('checkin-photos', 'checkin-photos', false)
ON CONFLICT (id) DO NOTHING;

CREATE POLICY "checkin_photos_owner_select" ON storage.objects
    FOR SELECT USING (bucket_id = 'checkin-photos' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "checkin_photos_owner_insert" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'checkin-photos' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "checkin_photos_owner_delete" ON storage.objects
    FOR DELETE USING (bucket_id = 'checkin-photos' AND (storage.foldername(name))[1] = auth.uid()::text);
