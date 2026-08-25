ALTER TABLE treatment_record_photos
    ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_treatment_record_photos_record_sort
    ON treatment_record_photos(record_id, sort_order, created_at, photo_id);
