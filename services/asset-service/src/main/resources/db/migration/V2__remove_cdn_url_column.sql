-- Remove cdn_url column as it can be computed from storage_path
-- CDN URL = {cdnBaseUrl}/{storagePath}

ALTER TABLE asset.asset DROP COLUMN cdn_url;

-- Update comment
COMMENT ON COLUMN asset.asset.storage_path IS 'S3 object key in R2 bucket. CDN URL is computed as {cdnBaseUrl}/{storagePath}';