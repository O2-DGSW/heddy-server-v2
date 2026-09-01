-- 기록 추가 화면의 "소요 시간"·"시술 내용" 입력란에 대응하는 컬럼. 지금까지는 두 값을 받아도
-- 저장할 자리가 없었다.
--
-- treatment_content 는 memo 와 별개다. 화면에도 두 입력란이 따로 있고, 시술 내용은 무엇을
-- 했는지(예: 애쉬브라운 전체 염색)를, memo 는 그 밖의 개인 기록을 담는다. 한 컬럼으로 합치면
-- 나중에 시술 내용만 뽑아 쓸 수 없다.
--
-- 길이는 v1 명세의 VARCHAR(255) 를 따른다. memo 처럼 TEXT 로 열어 두지 않는 이유는 시술
-- 내용이 한 줄 요약이기 때문이다 — 길어지는 내용은 memo 가 받는다.
ALTER TABLE treatment_records
    ADD COLUMN duration_minutes SMALLINT,
    ADD COLUMN treatment_content VARCHAR(255);
