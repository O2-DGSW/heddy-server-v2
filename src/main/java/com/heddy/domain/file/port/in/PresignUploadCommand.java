package com.heddy.domain.file.port.in;

import com.heddy.domain.file.model.FilePurpose;

import java.util.Objects;
import java.util.UUID;

/**
 * 업로드 세션 발급 입력. 크기·파일명·해시는 클라이언트가 <em>선언한</em> 값이다.
 *
 * <p>선언값은 완료 시점에 실측으로 재검증되므로 여기서 틀려도 최종 안전망은 남아 있다. 그래도
 * 명백히 규격 밖의 선언(허용 목록 밖 형식, 최대치 초과)은 발급 단계에서 거른다. 파일명은 오브젝트
 * 키 생성에 쓰지 않고 감사·표시 목적으로만 보관한다.
 */
public record PresignUploadCommand(
        UUID userId,
        FilePurpose purpose,
        String contentType,
        String fileName,
        long fileSize,
        String sha256
) {
    public PresignUploadCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(sha256, "sha256");
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize 는 양수여야 합니다: " + fileSize);
        }
    }
}
