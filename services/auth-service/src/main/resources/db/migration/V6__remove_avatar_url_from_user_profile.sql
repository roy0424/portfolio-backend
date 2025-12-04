-- Remove avatar_url column from user_profile table
-- We now fetch avatar URL dynamically from asset-service via gRPC
-- Only avatarAssetId is stored in the database

ALTER TABLE auth.user_profile
    DROP COLUMN IF EXISTS avatar_url;