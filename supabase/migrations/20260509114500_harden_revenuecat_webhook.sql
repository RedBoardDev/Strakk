-- RevenueCat webhook hardening
-- - Idempotency ledger for webhook events
-- - Additional subscription consistency constraints

CREATE TABLE IF NOT EXISTS revenuecat_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id TEXT NOT NULL UNIQUE,
    app_user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    payload_hash TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processing_status TEXT NOT NULL DEFAULT 'processed'
        CHECK (processing_status IN ('processed', 'ignored', 'failed')),
    details JSONB
);

CREATE INDEX IF NOT EXISTS idx_revenuecat_webhook_events_user_id
    ON revenuecat_webhook_events(app_user_id);

CREATE INDEX IF NOT EXISTS idx_revenuecat_webhook_events_processed_at
    ON revenuecat_webhook_events(processed_at DESC);

ALTER TABLE subscriptions
    ADD CONSTRAINT subscriptions_trial_requires_trial_end
    CHECK (
        status <> 'trial'
        OR (trial_end IS NOT NULL AND plan IS NULL)
    ) NOT VALID;

ALTER TABLE subscriptions
    ADD CONSTRAINT subscriptions_active_requires_plan_and_period_end
    CHECK (
        status <> 'active'
        OR (plan IS NOT NULL AND current_period_end IS NOT NULL)
    ) NOT VALID;

ALTER TABLE subscriptions
    ADD CONSTRAINT subscriptions_non_active_has_no_plan
    CHECK (
        status IN ('active', 'payment_failed')
        OR plan IS NULL
    ) NOT VALID;

ALTER TABLE subscriptions VALIDATE CONSTRAINT subscriptions_trial_requires_trial_end;
ALTER TABLE subscriptions VALIDATE CONSTRAINT subscriptions_active_requires_plan_and_period_end;
ALTER TABLE subscriptions VALIDATE CONSTRAINT subscriptions_non_active_has_no_plan;

NOTIFY pgrst, 'reload schema';
