package com.heddy.domain.account.port.in;

public record ResetPasswordCommand(String phoneNumber, String newPassword) {
}
