ALTER TABLE refresh_tokens
    DROP COLUMN IF EXISTS device_id,
    DROP COLUMN IF EXISTS platform,
    DROP COLUMN IF EXISTS app_version;
