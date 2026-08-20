package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.AuthProvider;
import com.heddy.domain.account.model.DeviceInfo;

public record SocialLoginCommand(
        AuthProvider provider,
        String providerToken,
        DeviceInfo device
) {
}
