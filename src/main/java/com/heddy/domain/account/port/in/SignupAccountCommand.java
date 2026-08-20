package com.heddy.domain.account.port.in;

public record SignupAccountCommand(String loginId, String password, String name, String phoneNumber) {
}
