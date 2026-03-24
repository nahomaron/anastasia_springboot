ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS workspace_initialization_mode VARCHAR(24),
    ADD COLUMN IF NOT EXISTS is_demo_workspace BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS scheduled_purge_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS purged_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS archive_scheduled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_tenants_scheduled_purge_at
    ON tenants(scheduled_purge_at);

CREATE INDEX IF NOT EXISTS idx_tenants_archive_scheduled_at
    ON tenants(archive_scheduled_at);
