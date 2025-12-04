-- Create asset schema
CREATE SCHEMA IF NOT EXISTS asset;

-- Asset metadata table
CREATE TABLE asset.asset (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL,
    site_id UUID,  -- Optional: can be NULL for user-level assets
    file_name VARCHAR(255) NOT NULL,  -- Generated storage filename (e.g., "abc123.jpg")
    original_file_name VARCHAR(255) NOT NULL,  -- Original uploaded filename
    content_type VARCHAR(100) NOT NULL,  -- MIME type (image/jpeg, video/mp4, etc.)
    file_size BIGINT NOT NULL,  -- Bytes
    storage_path VARCHAR(500) NOT NULL,  -- S3 key path (e.g., "public/uuid/abc123.jpg")
    cdn_url VARCHAR(500) NOT NULL,  -- Cloudflare CDN URL
    storage_provider VARCHAR(20) DEFAULT 'R2' NOT NULL,  -- Future-proof for multiple providers
    visibility VARCHAR(20) DEFAULT 'PRIVATE' NOT NULL,  -- PUBLIC or PRIVATE
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE, PROCESSING, FAILED, DELETED
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,  -- Soft delete

    -- Foreign key to auth schema (cross-schema FK)
    CONSTRAINT fk_user FOREIGN KEY (user_id)
        REFERENCES auth.user_account(id) ON DELETE CASCADE
);

-- Indexes for efficient queries
CREATE INDEX idx_asset_user_id ON asset.asset(user_id);
CREATE INDEX idx_asset_site_id ON asset.asset(site_id) WHERE site_id IS NOT NULL;
CREATE INDEX idx_asset_status ON asset.asset(status);
CREATE INDEX idx_asset_visibility ON asset.asset(visibility);
CREATE INDEX idx_asset_content_type ON asset.asset(content_type);
CREATE INDEX idx_asset_deleted_at ON asset.asset(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_asset_uploaded_at ON asset.asset(uploaded_at DESC);

-- Comments
COMMENT ON TABLE asset.asset IS 'File metadata for all uploaded assets';
COMMENT ON COLUMN asset.asset.storage_path IS 'S3 object key in R2 bucket';
COMMENT ON COLUMN asset.asset.cdn_url IS 'Cloudflare CDN URL for fast access';
COMMENT ON COLUMN asset.asset.visibility IS 'PUBLIC for published content, PRIVATE for drafts';
COMMENT ON COLUMN asset.asset.deleted_at IS 'Soft delete timestamp. NULL means active asset.';
