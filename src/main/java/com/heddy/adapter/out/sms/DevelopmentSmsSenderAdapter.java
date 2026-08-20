package com.heddy.adapter.out.sms;

import com.heddy.domain.account.port.out.SmsSenderPort;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "app.auth.sms.solapi",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DevelopmentSmsSenderAdapter implements SmsSenderPort {

    private final String webhookUrl;
    private final RestClient restClient;

    public DevelopmentSmsSenderAdapter(
            @Value("${app.auth.sms.development-webhook-url:}") String webhookUrl
    ) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public void send(String phoneNumber, String carrier, String code) {
        if (!StringUtils.hasText(webhookUrl)) {
            throw new AccountException(AccountError.SMS_SEND_FAILED);
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(Map.of("content", "Heddy 인증번호: %s".formatted(code)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            throw new AccountException(AccountError.SMS_SEND_FAILED);
        }
    }
}
