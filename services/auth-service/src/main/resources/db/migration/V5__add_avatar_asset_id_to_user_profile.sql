-- Add avatarAssetId column to user_profile table
ALTER TABLE auth.user_profile
    ADD COLUMN avatar_asset_id UUID;

-- Add comment
COMMENT ON COLUMN auth.user_profile.avatar_asset_id IS 'Asset ID for user avatar (references asset.asset.id)';
