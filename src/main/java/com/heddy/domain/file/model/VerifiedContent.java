package com.heddy.domain.file.model;

/**
 * 업로드 완료 시점에 실물을 검증하면서 알아낸 사실. 클라이언트가 선언한 값이 아니다.
 *
 * @param contentType 스토리지에 기록된 Content-Type
 * @param fileSize    실제 크기
 * @param sha256      내용 해시. 중복 판정과 무결성 확인에 쓴다
 * @param width       이미지 가로 픽셀
 * @param height      이미지 세로 픽셀
 */
public record VerifiedContent(
        String contentType,
        long fileSize,
        String sha256,
        int width,
        int height
) {
    private static final int SHA256_HEX_LENGTH = 64;

    public VerifiedContent {
        if (sha256 == null || sha256.length() != SHA256_HEX_LENGTH) {
            throw new IllegalArgumentException("sha256 은 64자 16진 문자열이어야 합니다");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("이미지 크기는 양수여야 합니다: %dx%d".formatted(width, height));
        }
    }
}
