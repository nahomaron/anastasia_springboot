CREATE TABLE image_asset_upload_intents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    image_asset_type VARCHAR(24) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    uploaded_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_image_asset_upload_intents_tenant_owner
    ON image_asset_upload_intents (tenant_id, owner_id, image_asset_type);

CREATE INDEX idx_image_asset_upload_intents_uploaded_by
    ON image_asset_upload_intents (uploaded_by_user_id);
