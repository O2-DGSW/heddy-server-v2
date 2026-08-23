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
    ANALYSIS_OVERLAY_INTERNAL(5L * 1024 * 1024, Set.of("image/png"));

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
        return allowedContentTypes.contains(contentType);
    }

    public boolean exceedsMaximum(long byteSize) {
        return byteSize > maximumBytes;
    }
}
