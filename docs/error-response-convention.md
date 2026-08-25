# 에러 응답 컨벤션 — 리소스 소유자 불일치 (#67)

요청자가 소유하지 않은 리소스에 접근했을 때 어떤 상태 코드로 답할지 정한다. 이 문서는 결정과 근거를 남겨서 이후 신규 API가 논쟁 없이 같은 규칙을 따르게 하는 것이 목적이다.

---

## 1. 확정 컨벤션

**소유자 불일치는 미존재와 똑같이 `404 RESOURCE_NOT_FOUND` 로 답한다.**

- 조회 질의에 소유자 조건을 함께 넣어(예: `findByIdAndUserId(recordId, requesterId)`) 미존재와 남의 리소스가 코드 경로까지 동일하게 처리되게 한다. 응답뿐 아니라 DB 질의 횟수·지연도 같아야 존재 여부가 새지 않는다.
- `403 FORBIDDEN_RESOURCE` 는 소유권 판단에 쓰지 않는다. 이 코드는 Spring Security 의 경로·권한 인가 실패(`RestAccessDeniedHandler`)처럼 **요청자가 누군지 몰라도 거부할 수 있는** 경우에만 쓴다.

## 2. 결정 근거

- **IDOR 방지** — 403 으로 답하면 "리소스는 존재하지만 네 것이 아니다"라고 알려준다. 식별자를 추측하는 공격자에게 유효한 ID 목록을 대신 걸러주는 셈이다. 404 은닉은 존재 신호 자체를 없앤다.
- **기구축 모듈 일관성** — 시술기록 모듈(PR #52, #55, #56, #63, #65)부터 `findByIdAndUserId` 로 404 은닉을 구현해 왔다(#31 리뷰 합의). 이미 운영 중인 API 군을 표준으로 삼는 쪽이 변경 비용과 혼란이 적다.
- **클라이언트 단순화** — 클라이언트는 "없다/남의 것이다"를 구분해 처리할 이유가 없다. 두 경우 모두 같은 에러 화면으로 끝난다.

## 3. as-built 준수 현황 (2026-08-25 기준)

아래 항목은 이 컨벤션 이전에 구축돼 아직 `403` 으로 답한다. **코드 변경은 이 문서에서 하지 않으며**, 후속 이슈로 분리해 수정한다.

| API | 위치 | 현재 응답 (소유자 불일치) |
|---|---|---|
| `POST /uploads/{uploadId}/complete` | `UploadSessionService.complete` | `403 FORBIDDEN_RESOURCE` |
| `DELETE /uploads/{uploadId}` | `UploadSessionService.cancel` | `403 FORBIDDEN_RESOURCE` |
| `POST /treatment-records` · `POST /treatment-records/{recordId}/photos` 의 첨부 파일 검증 | `TreatmentRecordService.requireOwnedReadyFile` | `403 FORBIDDEN_RESOURCE` |

시술기록 본체 CRUD·사진 관리(`get`, `update`, `delete`, 사진 추가/수정/삭제, 비교 조회)는 이미 `findByIdAndUserId` 기반 404 은닉으로 컨벤션을 준수한다.
