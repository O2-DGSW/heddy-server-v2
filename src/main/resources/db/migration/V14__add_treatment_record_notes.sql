-- 시술기록 수정 API에서 관리하는 개인 메모와 다음 방문 주의사항.
ALTER TABLE treatment_records
    ADD COLUMN memo TEXT,
    ADD COLUMN next_visit_cautions TEXT;
