package com.heddy.domain.account.port.in;

public record EmailLoginCommand(String email, String password) {
}
