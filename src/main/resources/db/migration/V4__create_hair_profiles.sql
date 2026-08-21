CREATE TABLE hair_profiles (
    hair_profile_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    hair_type VARCHAR(20) NOT NULL,
    hair_condition VARCHAR(20) NOT NULL,
    hair_length VARCHAR(20) NOT NULL,
    hair_thickness VARCHAR(20) NOT NULL,
    available_care_time_minutes INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_hair_profiles_care_time
        CHECK (available_care_time_minutes IS NULL OR available_care_time_minutes >= 0)
);
