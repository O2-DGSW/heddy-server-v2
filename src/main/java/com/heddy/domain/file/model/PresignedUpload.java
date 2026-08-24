package com.heddy.domain.file.model;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * 스토리지에 객체를 직접 쓰기 위해 클라이언트가 보내야 하는 요청 한 벌.
 *
 * <p>URL 만으로는 부족하다. 조건부 헤더({@code If-None-Match: *})는 서명에 포함돼 있으므로
 * 클라이언트가 반드시 함께 보내야 하고, 어떤 헤더가 그런지는 발급자가 알려줘야 한다.
 * {@code requiredHeaders} 가 그 안내다.
 */
public record PresignedUpload(
        URI url,
        String method,
        Map<String, String> requiredHeaders
) {
    public PresignedUpload {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(requiredHeaders, "requiredHeaders");
        requiredHeaders = Map.copyOf(requiredHeaders);
    }
}
