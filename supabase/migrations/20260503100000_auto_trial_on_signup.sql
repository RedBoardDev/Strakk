-- Auto-create a 7-day trial subscription when a new user signs up.
-- Updates handle_new_user() to also insert into subscriptions.

CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO profiles (id)
    VALUES (NEW.id)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO subscriptions (user_id, status, trial_end)
    VALUES (NEW.id, 'trial', now() + interval '7 days')
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$;
