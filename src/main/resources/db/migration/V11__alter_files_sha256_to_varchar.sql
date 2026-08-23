-- sha256 을 CHAR(64) 에서 VARCHAR(64) 로 바꾼다.
--
-- PostgreSQL 의 char(n) 은 bpchar 로 저장돼 값이 짧으면 공백으로 채워진다. 64자 16진 문자열만
-- 넣을 것이라 실제로 패딩이 생기지는 않지만, JPA 기본 문자열 매핑이 varchar 라 ddl-auto=validate
-- 가 타입 불일치로 부팅을 막는다. 매핑에 columnDefinition 을 덧붙여 맞추는 대신 컬럼을 바꾼다 --
-- 고정 길이라는 사실은 이미 애플리케이션이 검증하고 있고, 여기서 얻을 것이 없다.
ALTER TABLE files
    ALTER COLUMN sha256 TYPE VARCHAR(64);
