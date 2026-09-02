-- files.file_name 을 NOT NULL 로 좁힌다.
--
-- V13 이 이 컬럼을 nullable 로 추가한 것은 그 이전에 만들어진 행을 남겨두기 위해서였다.
-- 그런데 도메인 모델(StoredFile)은 fileName 을 필수로 요구한다. 두 계약이 어긋난 탓에 값이
-- 없는 행을 도메인으로 옮기는 순간 NullPointerException 이 나고, 페이지에 속한 파일을 한 번에
-- 읽는 시술기록 목록 조회가 통째로 500 이 됐다.
--
-- 컬럼을 비워둘 수 있게 두는 대신 값을 채우는 쪽으로 정리한다. 도메인이 필수로 보는 값을
-- 스키마가 비워둘 수 있으면 같은 사고가 단건 조회·사진 관리·파일 정리에서 다시 난다.
-- 지금 코드의 파일 생성 경로(StoredFile.pending · pendingSystem)는 모두 값을 채우므로,
-- 제약을 좁혀도 새로 막히는 흐름은 없다.
--
-- 남은 행의 이름은 object_key 의 마지막 세그먼트에서 되살린다. 키 형식이
-- {purpose}/{ownerId}/{uuid}.{ext} 라 확장자가 보존되고 행마다 고유하다. 원본 파일명은
-- 어디에도 남아 있지 않으므로, 그럴듯한 이름을 지어내기보다 실제로 저장된 객체의 이름을 쓴다.
-- file_name 은 감사·표시 용도일 뿐 오브젝트 키 생성에 쓰이지 않아(V13) 이 값으로 바뀌어도
-- 스토리지 접근 경로에는 영향이 없다.
UPDATE files
SET file_name = left(COALESCE(NULLIF(split_part(object_key, '/', -1), ''), 'unknown'), 255)
WHERE file_name IS NULL;

ALTER TABLE files ALTER COLUMN file_name SET NOT NULL;
