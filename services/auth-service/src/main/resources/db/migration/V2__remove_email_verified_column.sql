-- Remove email_verified column as it's redundant with email nullability
ALTER TABLE auth.user_account DROP COLUMN email_verified;
