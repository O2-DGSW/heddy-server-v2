package com.heddy.domain.file.port.out;

import com.heddy.domain.file.model.PresignedUpload;
import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;

import java.net.URI;
import java.util.Optional;

/**
 * 오브젝트 스토리지 추상화. 서버는 이미지 바이너리를 중계하지 않고 짧게 사는 URL 만 발급한다.
 */
public interface FileStoragePort {

    /**
     * 클라이언트가 객체를 직접 올릴 PUT 요청. 발급 시점의 Content-Type 과 "이미 존재하는 객체를
     * 덮어쓰지 마라"는 조건({@code If-None-Match: *})을 함께 서명한다. 클라이언트는 결과의
     * {@code requiredHeaders} 를 빠뜨리지 말고 보내야 한다.
     */
    PresignedUpload createUploadUrl(StoredFile file);

    /** 조회용 GET URL. 저장하지 않고 볼 때마다 발급한다. */
    URI createDownloadUrl(StoredFile file);

    /** 객체의 실제 상태. 올라온 적이 없으면 비어 있다. */
    Optional<StorageObject> findObject(String objectKey);
}
