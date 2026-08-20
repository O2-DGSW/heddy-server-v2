# 레이어·패키지 컨벤션 (v2)

v2는 v1의 헥사고날 구조를 **승계하지 않는다.** 도메인별 패키지 안에서 얇은 3계층
(`controller` → `service` → `repository`)을 쓰고, 공통 규약은 `global` 아래에 모은다.

## 1. 왜 헥사고날을 버렸나

v1은 도메인마다 `application/port/in`, `application/port/out`, `adapter/in/web`,
`adapter/out/persistence` 를 두어 엔드포인트 하나에 인터페이스 4개가 따라붙었다.
v2는 공개 엔드포인트 57개를 소수 인원이 짧은 기간에 붙여야 하고, 외부 어댑터를
갈아끼울 요구(다른 DB·다른 프로토콜)가 없다. 포트 추상화의 비용만 남고 이득이 없어
표준 Spring 3계층으로 되돌린다.

교체 가능성이 실제로 있는 지점(S3 · AI 서버 · Push)만 **선택적으로** 인터페이스를 둔다.

## 2. 패키지 구조

```
com.heddy
├── HeddyApplication.java
├── global/                     ← 도메인 무관 공통 규약. 도메인 패키지를 import 하지 않는다
│   ├── config/                 SecurityConfig, JpaConfig, OpenApiConfig …
│   ├── entity/                 BaseEntity
│   ├── error/                  ErrorCode, ApplicationException, ApiErrorResponse, GlobalExceptionHandler
│   ├── filter/                 RequestIdFilter
│   └── response/               ApiResponse, PageResponse
├── account/                    ← 도메인 패키지
│   ├── controller/             @RestController + 요청·응답 DTO
│   ├── service/                @Service, @Transactional 경계
│   ├── repository/             Spring Data JPA 인터페이스
│   └── entity/                 JPA 엔티티, enum
├── treatment/
├── analysis/
├── style/
├── sharing/
├── reservation/
└── notification/
```

- **도메인 패키지는 `global` 을 import 해도 되지만 그 반대는 금지.**
- 도메인 간 참조는 `service` 계층끼리만 한다. 남의 도메인 `repository` 를 직접 부르지 않는다.
- DTO 는 그 DTO 를 쓰는 컨트롤러 옆(`controller` 패키지)에 둔다. `dto` 전용 최상위 패키지를 만들지 않는다.
- 요청·응답 DTO 는 `record` 로 만든다. 엔티티를 그대로 응답에 노출하지 않는다.

## 3. 계층별 책임

| 계층 | 책임 | 금지 |
|---|---|---|
| `controller` | 요청 검증(`@Valid`), DTO ↔ 서비스 인자 변환, `ApiResponse` 래핑 | 비즈니스 분기, 트랜잭션, 엔티티 직접 노출 |
| `service` | 비즈니스 규칙, 트랜잭션 경계(`@Transactional`), 소유권 검증 | `HttpServletRequest` 등 웹 타입 의존 |
| `repository` | 영속성 접근 | 비즈니스 분기 |

- 조회 전용 서비스 메서드에는 `@Transactional(readOnly = true)` 를 붙인다.
- 소유권 검증은 **service 계층**이 한다. 본인 리소스가 아니면 `FORBIDDEN_RESOURCE`,
  애초에 존재를 숨겨야 하는 경우에만 `RESOURCE_NOT_FOUND` 를 쓴다.
- Spring 의 `AccessDeniedException`(`@PreAuthorize` 거부 포함)도 공통 핸들러가 받아
  익명이면 401 `AUTHENTICATION_REQUIRED`, 인증된 사용자면 403 `FORBIDDEN_RESOURCE` 로 내린다.
  둘 중 어느 쪽을 던져도 응답 포맷은 같다.

## 4. API 공통 규약

### URL

- 모든 공개 엔드포인트는 `/api/v1` prefix 를 붙인다.
  API 명세는 prefix 없이 `/treatment-records` 로 적혀 있으나, 버전 협상을 URL 로 하기로 하고
  스켈레톤 기준인 `/api/v1` 을 유지한다. 명세의 `/treatment-records` → 실제 `/api/v1/treatment-records`.

### JSON 네이밍

- 요청·응답 JSON 필드는 **snake_case**. `spring.jackson.property-naming-strategy: SNAKE_CASE` 로
  전역 적용돼 있으므로 **DTO 필드는 자바 관례대로 camelCase 로 쓴다.** `@JsonProperty` 를 개별로 붙이지 않는다.
- `null` 필드는 응답에서 생략된다(`default-property-inclusion: non_null`).

### 성공 응답

```java
@GetMapping("/{recordId}")
ApiResponse<TreatmentRecordResponse> get(@PathVariable UUID recordId) {
    return ApiResponse.of(service.get(recordId));
}
```

```json
{ "data": { ... }, "request_id": "..." }
```

- 본문이 없는 응답(`204 No Content`)은 래핑하지 않는다.
- 목록은 `ApiResponse<PageResponse<T>>` 로 감싼다.

```json
{
  "data": {
    "items": [],
    "page": { "number": 0, "size": 20, "total_elements": 43, "total_pages": 3, "has_next": true }
  },
  "request_id": "..."
}
```

### 에러 응답

```json
{
  "error": { "code": "STYLE_PREFERENCE_CONFLICT", "message": "...", "field_errors": [{ "field": "...", "reason": "..." }] },
  "request_id": "..."
}
```

- 에러는 `throw new ApplicationException(코드)` 로만 발생시킨다.
  컨트롤러에서 `ResponseEntity` 로 에러를 직접 조립하지 않는다.
- 필드 단위 사유가 있으면 3-인자 생성자로 `field_errors` 를 채운다. 비어 있으면 응답에서 생략된다.
- 400(`INVALID_REQUEST`)은 파싱·바인딩 실패, 422(`VALIDATION_FAILED`)는 필드 검증 실패로 구분한다.

#### 오류 코드 추가 규칙

`ErrorCode` 는 **인터페이스**다. 공통 코드(명세 §18.1) 9종만 `global/error/CommonErrorCode` 에 있고,
도메인 코드는 각 도메인이 자기 패키지에 enum 을 만들어 추가한다. 공통 enum 에 도메인 코드를 넣지 않는다.

```java
package com.heddy.treatment.error;

public enum TreatmentErrorCode implements ErrorCode {

    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "시술기록을 찾을 수 없습니다."),
    RECORD_FUTURE_DATE_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_CONTENT, "미래 일시로는 등록할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
    // 생성자 + status() + message() 오버라이드
}
```

`code()` 는 인터페이스 기본 구현이 enum 이름을 그대로 돌려주므로 직접 만들지 않는다.
(enum 이 아닌 구현체는 `code()` 호출 시 `IllegalStateException` 으로 막힌다.)

**위치** — `com.heddy.<도메인>.error.<도메인>ErrorCode`. 도메인 하나에 enum 하나.

**명명 규칙**

- `SCREAMING_SNAKE_CASE`, 영문만. **명세 §18 에 적힌 문자열을 글자 그대로 쓴다.**
- 명세에 없는 코드가 필요하면 **API 명세 §18 을 먼저 갱신**하고 enum 에 반영한다. 코드부터 만들지 않는다.
- 앞부분은 리소스·도메인(`AUTH_` `UPLOAD_` `RECORD_` `PHOTO_` `ANALYSIS_` `SHARE_` …),
  뒷부분은 상태·사유(`_NOT_FOUND` `_EXPIRED` `_LIMIT_EXCEEDED` `_INVALID` `_ALREADY_EXISTS`).
- 같은 뜻을 공통 코드가 이미 갖고 있으면 도메인 코드를 새로 만들지 않는다.
  단순 "없음"·"권한 없음"은 `RESOURCE_NOT_FOUND` · `FORBIDDEN_RESOURCE` 를 쓴다.
  클라이언트가 화면 분기를 해야 할 때만 도메인 코드로 쪼갠다.

**HTTP status 매핑 기준** (명세 §2.4)

| 상태 | 언제 | 예 |
|---|---|---|
| `400` | JSON 파싱 실패, 파라미터 형식 오류 | `INVALID_REQUEST` |
| `401` | 토큰 없음·만료·위조, 자격 증명 불일치 | `AUTHENTICATION_REQUIRED`, `AUTH_INVALID_CREDENTIALS` |
| `403` | 인증은 됐으나 본인 리소스가 아님, 계정 상태 차단 | `FORBIDDEN_RESOURCE`, `AUTH_ACCOUNT_LOCKED` |
| `404` | 리소스 없음 | `RESOURCE_NOT_FOUND`, `RECORD_NOT_FOUND` |
| `409` | 중복 생성, 이미 실행 중 | `AUTH_EMAIL_ALREADY_EXISTS`, `ANALYSIS_ALREADY_RUNNING` |
| `413` | 업로드 크기 초과 | `UPLOAD_FILE_TOO_LARGE` |
| `415` | 지원하지 않는 형식 | `UPLOAD_MEDIA_TYPE_UNSUPPORTED` |
| `422` | 형식은 맞지만 비즈니스 규칙 위반 (기본값) | `VALIDATION_FAILED`, `RECORD_FUTURE_DATE_NOT_ALLOWED` |
| `429` | 요청·재시도 제한 초과 | `RATE_LIMIT_EXCEEDED` |
| `500` | 서버 내부 오류 | `INTERNAL_SERVER_ERROR` |
| `502` | 외부 소셜·AI 서버 오류 | `ANALYSIS_SERVER_ERROR` |
| `503` | S3·AI·Push 일시 장애 | `DEPENDENCY_UNAVAILABLE` |

- **어디에 넣을지 애매하면 422** 를 쓴다. 400 은 요청을 파싱조차 못 한 경우로 좁게 유지한다.
- 존재 자체를 숨겨야 하는 리소스(남의 공유 링크 등)는 403 대신 404 를 쓴다.

### X-Request-Id

- `RequestIdFilter` 가 보안 필터 체인보다 먼저 실행되며, 클라이언트가 `X-Request-Id` 를 보내면
  그 값을, 없으면 UUID 를 발급한다. 값은 MDC 키 `requestId` 와 응답 헤더에 동시에 실린다.
- 로그 레벨 패턴에 `%X{requestId}` 가 들어 있어 모든 로그에 자동으로 붙는다. 직접 로그에 찍지 않는다.
- 응답 본문의 `request_id` 는 `ApiResponse.of()` / `ApiErrorResponse.of()` 가 알아서 채운다.

## 5. 테스트

- 컨트롤러: `@WebMvcTest` + `MockMvc`. 응답 JSON 은 `$.data.*` · `$.error.code` 경로로 검증한다.
- 서비스: 순수 단위 테스트(모킹).
- 통합: `@SpringBootTest` + H2 인메모리(`application-test.yml`). 컨테이너를 띄우지 않는다.
