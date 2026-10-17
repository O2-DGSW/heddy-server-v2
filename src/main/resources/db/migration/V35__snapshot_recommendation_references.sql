-- Preserve the evidence shown when a recommendation was generated.
-- Historical originals are unavailable; freeze the current values for legacy runs.
ALTER TABLE recommendation_reference_records
    ADD COLUMN performed_at_snapshot TIMESTAMP WITH TIME ZONE,
    ADD COLUMN satisfaction_snapshot INTEGER;

UPDATE recommendation_reference_records reference
SET performed_at_snapshot = record.performed_at,
    satisfaction_snapshot = record.satisfaction
FROM treatment_records record WHERE record.record_id = reference.record_id;

ALTER TABLE recommendation_reference_records ALTER COLUMN performed_at_snapshot SET NOT NULL;
