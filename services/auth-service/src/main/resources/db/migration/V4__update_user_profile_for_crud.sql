-- Add soft delete support and update user profile fields

-- Add deleted_at column for soft delete
ALTER TABLE auth.user_profile
    ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

-- Remove location field (not needed for portfolios)
ALTER TABLE auth.user_profile
    DROP COLUMN IF EXISTS location;

-- Update display_name max length to 20 characters
ALTER TABLE auth.user_profile
    ALTER COLUMN display_name TYPE VARCHAR(20);

-- Add index on deleted_at for performance
CREATE INDEX idx_user_profile_deleted_at ON auth.user_profile(deleted_at) WHERE deleted_at IS NULL;

-- Add comment
COMMENT ON COLUMN auth.user_profile.deleted_at IS 'Soft delete timestamp. NULL means active profile.';