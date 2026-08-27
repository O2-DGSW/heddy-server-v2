package com.heddy.adapter.in.web.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.account.model.DeviceInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Refresh Token 세션을 기기별로 식별하고 관리하기 위한 정보")
public record DeviceRequest(
        @NotBlank @Size(max = 100) @JsonProperty("device_id")
        @Schema(description = "앱이 생성해 기기에 보관하는 고유 ID", example = "local-device-uuid")
        String deviceId,
        @NotNull
        @Schema(description = "클라이언트 플랫폼", example = "IOS")
        DeviceInfo.Platform platform,
        @NotBlank @Size(max = 20) @JsonProperty("app_version")
        @Schema(description = "앱 버전", example = "1.0.0")
        String appVersion
) {
    public DeviceInfo toDomain() {
        return new DeviceInfo(deviceId, platform, appVersion);
    }
}
