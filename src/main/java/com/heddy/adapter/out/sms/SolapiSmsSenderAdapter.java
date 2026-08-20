package com.heddy.adapter.out.sms;

import com.heddy.domain.account.port.out.SmsSenderPort;
import com.heddy.domain.account.exception.AccountError;
import com.heddy.domain.account.exception.AccountException;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.auth.sms.solapi", name = "enabled", havingValue = "true")
public class SolapiSmsSenderAdapter implements SmsSenderPort {

    private static final String API_URL = "https://api.solapi.com";

    private final DefaultMessageService messageService;
    private final String from;

    public SolapiSmsSenderAdapter(
            @Value("${app.auth.sms.solapi.api-key}") String apiKey,
            @Value("${app.auth.sms.solapi.api-secret}") String apiSecret,
            @Value("${app.auth.sms.solapi.from}") String from
    ) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, API_URL);
        this.from = from;
    }

    @Override
    public void send(String phoneNumber, String carrier, String code) {
        Message message = new Message();
        message.setFrom(from);
        message.setTo(phoneNumber);
        message.setText("[Heddy] 인증번호 [%s]를 입력해주세요.".formatted(code));
        try {
            messageService.send(message);
        } catch (Exception exception) {
            throw new AccountException(AccountError.SMS_SEND_FAILED);
        }
    }
}
