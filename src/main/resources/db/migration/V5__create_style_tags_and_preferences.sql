CREATE TABLE style_tags (
    style_tag_id UUID PRIMARY KEY,
    tag_name VARCHAR(30) NOT NULL,
    category VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_style_tags_name_category UNIQUE (tag_name, category),
    CONSTRAINT ck_style_tags_category CHECK (
        category IN ('BANG', 'SHORT', 'BOB', 'MEDIUM', 'LONG', 'UPDO')
    )
);

CREATE TABLE user_style_preferences (
    preference_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    style_tag_id UUID NOT NULL REFERENCES style_tags(style_tag_id) ON DELETE CASCADE,
    preference_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_style_preferences_user_tag UNIQUE (user_id, style_tag_id),
    CONSTRAINT ck_user_style_preferences_type CHECK (
        preference_type IN ('PREFERRED', 'EXCLUDED')
    )
);

CREATE INDEX idx_user_style_preferences_user_id
    ON user_style_preferences(user_id);
