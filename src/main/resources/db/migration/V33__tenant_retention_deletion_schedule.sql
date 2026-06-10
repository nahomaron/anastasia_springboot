ALTER TABLE tenants ADD COLUMN IF NOT EXISTS scheduled_deletion_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_tenants_scheduled_deletion_at ON tenants(scheduled_deletion_at);
