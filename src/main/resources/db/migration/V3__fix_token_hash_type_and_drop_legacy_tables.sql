-- Hibernate 는 자바 String 을 항상 Types#VARCHAR 로 매핑한다. DDL 이 CHAR 인 한
-- ddl-auto: validate 가 매번 실패해 애플리케이션이 기동되지 않는다.
-- CHAR 는 짧은 값을 공백으로 채워 VARCHAR 와 비교 시 패딩 세만틱까지 달라진다.
-- TRIM 은 과거에 64 자 미만으로 저장된 값이 있어도 패딩이 남지 않게 한다.
ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64) USING TRIM(TRAILING FROM token_hash);

-- V1 이 만들고 V2 가 이름만 바꾼 뒤 한 번도 쓰이지 않는 테이블이다.
-- 이름만 바뀌었으므로 accounts_pkey 같은 제약·인덱스 이름도 그대로 남아 있다.
-- legacy_social_accounts 가 legacy_accounts 를 참조하므로 자식부터 삭제한다.
DROP TABLE IF EXISTS legacy_social_accounts;
DROP TABLE IF EXISTS legacy_accounts;
