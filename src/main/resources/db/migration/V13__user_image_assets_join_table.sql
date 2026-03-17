CREATE TABLE users_image_assets (
    user_entity_uuid UUID NOT NULL,
    image_assets_id BIGINT NOT NULL,
    CONSTRAINT pk_users_image_assets PRIMARY KEY (user_entity_uuid, image_assets_id),
    CONSTRAINT fk_users_image_assets_user FOREIGN KEY (user_entity_uuid) REFERENCES users(uuid) ON DELETE CASCADE,
    CONSTRAINT fk_users_image_assets_image FOREIGN KEY (image_assets_id) REFERENCES image_assets(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_users_image_assets_image ON users_image_assets(image_assets_id);
CREATE INDEX idx_users_image_assets_user ON users_image_assets(user_entity_uuid);
