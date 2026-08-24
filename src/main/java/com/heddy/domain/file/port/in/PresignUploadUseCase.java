package com.heddy.domain.file.port.in;

import java.util.UUID;

/** POST /uploads/presign. 발급 전 purpose 별 제약(허용 형식·최대 크기)을 검증한다. */
public interface PresignUploadUseCase {

    PresignUploadResult presign(PresignUploadCommand command);
}
