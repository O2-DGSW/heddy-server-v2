package com.heddy.global.response;

public record ApiResponse<T>(T data, String message) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null, null);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(null, message);
    }
}
