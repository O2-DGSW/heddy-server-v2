package com.heddy.domain.file.model;

import java.util.Set;

/**
 * 파일의 용도. 용도마다 허용 크기와 Content-Type 이 다르므로 제약을 여기에 함께 둔다.
 *
 * <p>제약을 설정값이 아니라 도메인에 두는 이유는, 이 값이 운영 중 조정하는 튜닝 파라미터가 아니라
 * "AR 캡처는 시술 사진보다 작다" 같은 도메인 사실이기 때문이다. 스프링 컨텍스트 없이 검증된다.
 */
public enum FilePurpose {

    /** 사용자가 올리는 시술 전·후 사진. AI 모발 분석의 입력이 된다. */
    TREATMENT_PHOTO(10L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/heic")),

    /** AR 체험 화면 캡처. 화면 해상도 산출물이라 원본 사진보다 작다. */
    AR_CAPTURE(5L * 1024 * 1024, Set.of("image/jpeg", "image/png")),

    /** 분석 서버가 생성하는 오버레이 이미지. 사용자에게 직접 노출되지 않는다. */
    ANALYSIS_OVERLAY_INTERNAL(5L * 1024 * 1024, Set.of("image/png")),

    /** 운영자가 검수해 등록하는 스타일 카탈로그 썸네일. */
    HAIRSTYLE_THUMBNAIL(5L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp")),
    /** AR 합성 원본과 색상 마스크는 사용자 업로드 API에서 발급하지 않는다. */
    HAIRSTYLE_AR_BASE(10L * 1024 * 1024, Set.of("image/png", "image/webp")),
    HAIRSTYLE_AR_MASK(10L * 1024 * 1024, Set.of("image/png"));

    private final long maximumBytes;
    private final Set<String> allowedContentTypes;

    FilePurpose(long maximumBytes, Set<String> allowedContentTypes) {
        this.maximumBytes = maximumBytes;
        this.allowedContentTypes = allowedContentTypes;
    }

    public long maximumBytes() {
        return maximumBytes;
    }

    public Set<String> allowedContentTypes() {
        return allowedContentTypes;
    }

    public boolean allows(String contentType) {
        // Set.of(...) 는 null 조회에 NPE 를 던진다. Content-Type 이 비어 온 요청은
        // 500 이 아니라 "허용되지 않는 형식"으로 떨어져야 한다.
        return contentType != null && allowedContentTypes.contains(contentType);
    }

    public boolean exceedsMaximum(long byteSize) {
        return byteSize > maximumBytes;
    }

    /**
     * 외부(앱) 업로드 API 가 발급받을 수 있는 용도인지.
     *
     * <p>{@code ANALYSIS_OVERLAY_INTERNAL} 은 분석 서버가 만드는 내부 생성물이다. 사용자가 같은
     * 용도로 객체를 발급해 올릴 수 있으면, 이후 단계는 이 purpose 를 "시스템이 만들고 검증한
     * 파일"로 신뢰할 근거를 잃는다. 그래서 내부 전용 용도는 외부 발급 경로에서 거부하고, 생성은
     * 나중에 생길 내부 전용 경로(분석 callback)가 맡는다.
     */
    public boolean isExternallyRequestable() {
        return this == TREATMENT_PHOTO || this == AR_CAPTURE;
    }
}
