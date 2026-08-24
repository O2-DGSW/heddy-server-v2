package com.heddy.domain.file.port.in;

import com.heddy.domain.file.model.FileStatus;
import com.heddy.domain.file.model.StoredFile;

import java.util.Objects;
import java.util.UUID;

/** 업로드 완료 결과. 재완료(멱등 재요청)일 때도 저장된 READY 상태가 그대로 돌아간다. */
public record CompleteUploadResult(
        UUID fileId,
        UUID uploadId,
        FileStatus status,
        String contentType,
        long fileSize,
        Integer width,
        Integer height
) {
    public CompleteUploadResult {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(uploadId, "uploadId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(contentType, "contentType");
    }

    /**
     * {@code width}·{@code height} 는 HEAD 로 알 수 없어 아직 비어 있다. 내용을 내려받아 확인하는
     * 검증 단위가 실측값을 채운 뒤부터 값이 나온다.
     */
    public static CompleteUploadResult from(StoredFile file) {
        return new CompleteUploadResult(
                file.fileId(), file.uploadId(), file.status(), file.contentType(),
                file.fileSize(), file.width(), file.height());
    }
}
