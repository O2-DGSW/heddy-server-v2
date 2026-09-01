package com.heddy.global.docs;

import com.heddy.global.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 엔드포인트마다 반복되는 응답 문서를 한곳에 모은다. 인증 필요·검증 실패·소유권처럼 거의 모든
 * API 가 같은 방식으로 답하는 것들이라, 40개 엔드포인트에 같은 문장을 복사해 두면 규칙이 바뀔 때
 * 일부만 고쳐져 문서가 서로 어긋난다.
 *
 * <p>엔드포인트에만 있는 실패는 여기 넣지 않고 그 자리에 직접 적는다.
 */
public final class ApiDocs {

    private ApiDocs() {
    }

    // ---------------------------------------------------------------- 성공

    /**
     * 성공 응답은 명시하지 않으면 사라진다. springdoc 은 실패 응답을 하나라도 선언한 순간
     * 자동으로 넣어 주던 성공 응답을 더 이상 넣지 않기 때문이다 — 본문 스키마는 여기서 비워 둬도
     * 반환 타입에서 그대로 추론된다.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(responseCode = "200", description = "성공"))
    public @interface Ok {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(responseCode = "201", description = "생성됨"))
    public @interface Created {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(responseCode = "202", description = "접수됨. 처리는 비동기로 이어진다"))
    public @interface Accepted {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(responseCode = "204", description = "성공. 본문 없음"))
    public @interface NoContent {
    }

    // ---------------------------------------------------------------- 실패

    /** 토큰이 필요한 모든 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "401",
            description = "AUTHENTICATION_REQUIRED — 토큰이 없거나 만료됐다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface Authenticated {
    }

    /** 요청 바디나 파라미터를 받는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "INVALID_REQUEST — 본문을 읽을 수 없거나 파라미터 형식이 "
                            + "잘못됐다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "VALIDATION_FAILED — 필드 검증에 실패했다. 어느 필드가 왜 "
                            + "걸렸는지는 error.field_errors 에 담긴다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface Validated {
    }

    /** 페이지·정렬·기간 조건을 받는 목록 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "400",
            description = "INVALID_REQUEST — 페이지·크기·정렬 값이 허용 범위를 벗어났거나 "
                    + "조회 시작이 종료보다 뒤다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface ListQuery {
    }

    /** 업로드한 파일을 첨부하는 API. 첨부 대상은 READY 인 본인 파일이어야 한다. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "404",
                    description = "RESOURCE_NOT_FOUND — 첨부한 file_id 가 없거나 내 파일이 아니다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "TREATMENT_PHOTO_LIMIT_EXCEEDED — 사진이 10장을 넘었다 · "
                            + "FILE_INVALID_STATE — 아직 READY 가 아닌 파일이다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface PhotoAttachment {
    }

    /** 전·후 사진이 모두 있어야 성립하는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "422",
            description = "PHOTO_COMPARISON_NOT_AVAILABLE — 전·후 사진 중 한쪽이라도 없다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface PhotoComparison {
    }

    /** 가입 API. 계정 식별자 중복과 필수 약관을 함께 본다. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "409",
                    description = "AUTH_EMAIL_ALREADY_EXISTS · AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED "
                            + "· AUTH_PHONE_ALREADY_EXISTS — 이미 쓰이는 식별자다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "CONSENT_REQUIRED_NOT_GRANTED — 필수 약관에 동의하지 않았다 · "
                            + "AUTH_WEAK_PASSWORD — 비밀번호 규칙을 어겼다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface Signup {
    }

    /** 자격 증명을 대조하는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "401",
                    description = "AUTH_INVALID_CREDENTIALS · AUTH_SOCIAL_TOKEN_INVALID — "
                            + "자격 증명이 맞지 않는다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "AUTH_ACCOUNT_DELETED — 탈퇴한 계정이다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "423",
                    description = "AUTH_ACCOUNT_LOCKED — 로그인 실패 누적으로 잠긴 계정이다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface Credentials {
    }

    /** 리프레시 토큰을 대조하는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "401",
            description = "AUTH_REFRESH_TOKEN_INVALID — 없거나 만료·철회된 토큰이다 · "
                    + "AUTH_REFRESH_TOKEN_REUSED — 이미 회전된 토큰을 다시 썼다. 이때는 그 "
                    + "사용자의 세션이 모두 무효화된다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface RefreshToken {
    }

    /** SMS 인증번호를 다루는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "404",
                    description = "AUTH_SMS_CODE_NOT_FOUND — 발송 기록이 없거나 만료됐다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "AUTH_SMS_CODE_INVALID — 인증번호가 틀렸다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "423",
                    description = "AUTH_SMS_CODE_MAX_ATTEMPTS — 시도 횟수를 넘겼다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "429",
                    description = "AUTH_SMS_SEND_TOO_SOON — 직전 발송 후 대기 시간이 남았다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "AUTH_SMS_SEND_FAILED — 발송에 실패했다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface SmsVerification {
    }

    /** 재인증 토큰을 요구하는 민감 작업. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "401",
            description = "AUTH_REAUTHENTICATION_REQUIRED — 재인증 토큰이 없거나 만료됐다 · "
                    + "AUTH_REAUTHENTICATION_TOKEN_REUSED — 이미 쓴 토큰이다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface Reauthenticated {
    }

    /**
     * 업로드 세션을 다루는 API. 남의 세션에 403 을 주는 유일한 곳이다 — 다른 도메인은 존재를
     * 감추려고 404 로 답한다.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN_RESOURCE — 내 업로드 세션이 아니다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "RESOURCE_NOT_FOUND — 없는 세션이다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface UploadSession {
    }

    /** 선호·제외 태그를 저장하는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "422",
            description = "STYLE_PREFERENCE_LIMIT_EXCEEDED — 선호·제외가 각각 10개를 넘었다 · "
                    + "STYLE_PREFERENCE_CONFLICT — 같은 태그를 선호와 제외에 함께 넣었다 · "
                    + "VALIDATION_FAILED — 존재하지 않는 태그가 섞였다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface StylePreference {
    }

    /** 동의를 변경하는 API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "422",
            description = "CONSENT_WITHDRAWAL_REQUIRES_ACCOUNT_DELETION — 필수 약관은 철회할 수 "
                    + "없고 탈퇴로만 거둘 수 있다 · CONSENT_POLICY_VERSION_INVALID — 현재 정책 "
                    + "버전과 다르다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface Consent {
    }

    /**
     * 토큰으로만 접근하는 공개 API. 인증이 필요 없는 대신 링크 상태를 실패로 구분해 답한다.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses({
            @ApiResponse(
                    responseCode = "404",
                    description = "SHARE_TOKEN_INVALID — 없는 토큰이다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "SHARE_REVOKED — 철회된 링크다 · SHARE_EXPIRED — 만료된 링크다",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public @interface PublicShare {
    }

    /**
     * 본인 리소스만 다루는 API. 남의 리소스는 존재 여부를 드러내지 않으려고 404 로 답한다 —
     * 403 을 주면 "그 식별자는 존재한다"는 사실이 새기 때문이다(업로드 API 는 예외로 403 이며
     * 해당 엔드포인트에 따로 적는다).
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @ApiResponses(@ApiResponse(
            responseCode = "404",
            description = "RESOURCE_NOT_FOUND — 없는 리소스이거나 내 리소스가 아니다",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))))
    public @interface OwnedResource {
    }
}
