package com.heddy.domain.file.port.out;

import com.heddy.domain.file.model.StorageObject;
import com.heddy.domain.file.model.StoredFile;

import java.net.URI;
import java.util.Optional;

/**
 * 오브젝트 스토리지 추상화. 서버는 이미지 바이너리를 중계하지 않고 짧게 사는 URL 만 발급한다.
 */
public interface FileStoragePort {

    /** 클라이언트가 객체를 직접 올릴 PUT URL. 발급 시점의 Content-Type 으로 서명한다. */
    URI createUploadUrl(StoredFile file);

    /** 조회용 GET URL. 저장하지 않고 볼 때마다 발급한다. */
    URI createDownloadUrl(StoredFile file);

    /** 객체의 실제 상태. 올라온 적이 없으면 비어 있다. */
    Optional<StorageObject> findObject(String objectKey);
}
