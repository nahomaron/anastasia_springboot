ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS is_demo_template BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_tenants_demo_template
    ON tenants(is_demo_template)
    WHERE is_demo_template = TRUE;
