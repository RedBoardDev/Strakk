-- Allow users to expire their own trial when trial_end has passed.
-- Restricted: can only set status to 'expired' on their own row.

CREATE POLICY "Users can expire own trial"
    ON subscriptions FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (
        auth.uid() = user_id
        AND status = 'expired'
    );
