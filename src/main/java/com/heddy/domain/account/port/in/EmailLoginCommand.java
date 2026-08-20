package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.DeviceInfo;

public record EmailLoginCommand(String email, String password, DeviceInfo device) {
}
