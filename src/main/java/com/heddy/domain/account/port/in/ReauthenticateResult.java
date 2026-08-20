package com.heddy.domain.account.port.in;

public record ReauthenticateResult(String reauthenticationToken, long expiresIn) {
}
