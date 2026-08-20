package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.DeviceInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank @Size(max = 100) @JsonProperty("device_id") String deviceId,
        @NotNull DeviceInfo.Platform platform,
        @NotBlank @Size(max = 20) @JsonProperty("app_version") String appVersion
) {
    public DeviceInfo toDomain() {
        return new DeviceInfo(deviceId, platform, appVersion);
    }
}
