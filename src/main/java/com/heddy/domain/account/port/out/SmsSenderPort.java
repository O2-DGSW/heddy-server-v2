package com.heddy.domain.account.port.out;

public interface SmsSenderPort {
    void send(String phoneNumber, String carrier, String code);
}
