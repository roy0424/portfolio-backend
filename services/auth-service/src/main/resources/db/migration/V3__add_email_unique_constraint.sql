-- Add UNIQUE constraint to email column
-- This ensures email uniqueness across the entire system
-- NULL values are allowed (multiple users can have NULL email)
-- but duplicate non-NULL emails are prevented
ALTER TABLE auth.user_account
ADD CONSTRAINT unique_user_account_email UNIQUE (email);