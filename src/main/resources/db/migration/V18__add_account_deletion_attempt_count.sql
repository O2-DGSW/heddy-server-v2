ALTER TABLE account_deletion_requests
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
