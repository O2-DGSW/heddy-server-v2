package com.heddy.domain.account.model;

public record DeviceInfo(String deviceId, Platform platform, String appVersion) {

    public enum Platform {
        IOS,
        ANDROID
    }
}
