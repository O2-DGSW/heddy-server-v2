package com.heddy.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse<T>(
        T data,
        @JsonProperty("request_id") String requestId
) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(data, requestId);
    }
}
