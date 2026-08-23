package com.heddy.domain.file.service;

import com.heddy.domain.file.model.FilePurpose;

import java.util.Map;
import java.util.UUID;

/**
 * 스토리지 객체 키를 만든다. {@code {purpose}/{ownerId}/{uuid}.{ext}} 형태다.
 *
 * <p><strong>클라이언트가 준 파일명을 키에 절대 넣지 않는다.</strong> 파일명에는 경로 구분자와
 * 상위 디렉터리 표기가 들어올 수 있고, 그대로 키에 붙이면 다른 사용자의 접두사 아래에 객체를
 * 쓸 수 있다. 확장자도 파일명에서 떼지 않고 Content-Type 에서 역산한다 — 이미 허용 목록으로
 * 검증된 값이라 임의 문자열이 섞이지 않는다.
 *
 * <p>소유자 id 를 접두사에 두는 이유는 나중에 버킷 정책이나 접두사 단위 수명 주기 규칙을
 * 사용자별로 걸 수 있게 하기 위해서다.
 */
public final class ObjectKeyGenerator {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/heic", "heic");

    private ObjectKeyGenerator() {
    }

    public static String generate(FilePurpose purpose, UUID ownerId, String contentType) {
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("확장자를 알 수 없는 Content-Type: " + contentType);
        }
        return "%s/%s/%s.%s".formatted(purpose.name(), ownerId, UUID.randomUUID(), extension);
    }
}
