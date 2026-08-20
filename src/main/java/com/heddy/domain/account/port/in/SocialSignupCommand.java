package com.heddy.domain.account.port.in;

public record SocialSignupCommand(String pendingToken, String name, String phoneNumber) {
}
