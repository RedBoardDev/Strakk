-- Subscription tracking — source of truth for user entitlements.
-- Only server (service role via RevenueCat webhook) can write.
-- Clients read via RLS-protected SELECT only.

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'free'
        CHECK (status IN ('free', 'trial', 'active', 'expired', 'payment_failed')),
    plan TEXT
        CHECK (plan IS NULL OR plan IN ('monthly', 'annual')),
    trial_end TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    revenuecat_customer_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id)
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);

-- Auto-update updated_at on any row change
CREATE OR REPLACE FUNCTION update_subscriptions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_subscriptions_updated_at();

-- RLS: users can ONLY READ their own subscription row
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own subscription"
    ON subscriptions FOR SELECT
    USING (auth.uid() = user_id);

-- NO insert/update/delete policies for authenticated role.
-- Only service_role (webhook Edge Function) can modify subscription state.
-- This makes the subscription system unbypassable from the client.

NOTIFY pgrst, 'reload schema';
