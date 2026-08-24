-- 클라이언트가 presign 요청으로 선언한 원본 파일명.
--
-- 오브젝트 키 생성에는 절대 쓰지 않는다(경로 조작 방지). 감사·표시 목적으로만 보관한다.
-- 명세의 presign 요청 계약(file_name 필수)을 따르므로 새 세션 행은 항상 값을 가지지만,
-- 기존 행과 내부 발급 경로를 해치지 않도록 nullable 로 둔다.
ALTER TABLE files ADD COLUMN file_name VARCHAR(255);
