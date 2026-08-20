package com.heddy.adapter.out.cache;

import com.heddy.domain.account.model.SmsVerification;
import com.heddy.domain.account.model.SmsVerificationPurpose;
import com.heddy.domain.account.port.out.SmsVerificationStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SmsVerificationRedisAdapter implements SmsVerificationStorePort {

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String COOLDOWN_KEY_PREFIX = "sms:cooldown:";
    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(1);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String phoneNumber, SmsVerification verification) {
        String value = "%s:%d:%d".formatted(
                verification.code(), verification.attempts(), verification.createdAt().toEpochMilli());
        redisTemplate.opsForValue().set(codeKey(phoneNumber), value, CODE_TTL);
    }

    @Override
    public Optional<SmsVerification> find(String phoneNumber) {
        String value = redisTemplate.opsForValue().get(codeKey(phoneNumber));
        if (value == null) {
            return Optional.empty();
        }
        try {
            String[] parts = value.split(":", 3);
            return Optional.of(new SmsVerification(
                    parts[0], Integer.parseInt(parts[1]), Instant.ofEpochMilli(Long.parseLong(parts[2]))));
        } catch (RuntimeException exception) {
            redisTemplate.delete(codeKey(phoneNumber));
            return Optional.empty();
        }
    }

    @Override
    public void delete(String phoneNumber) {
        redisTemplate.delete(codeKey(phoneNumber));
    }

    @Override
    public void startCooldown(String phoneNumber) {
        redisTemplate.opsForValue().set(COOLDOWN_KEY_PREFIX + phoneNumber, "1", COOLDOWN_TTL);
    }

    @Override
    public boolean hasCooldown(String phoneNumber) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_KEY_PREFIX + phoneNumber));
    }

    @Override
    public void markVerified(String phoneNumber, SmsVerificationPurpose purpose) {
        redisTemplate.opsForValue().set(verifiedKey(phoneNumber, purpose), "1", VERIFIED_TTL);
    }

    @Override
    public boolean isVerified(String phoneNumber, SmsVerificationPurpose purpose) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(phoneNumber, purpose)));
    }

    @Override
    public void deleteVerified(String phoneNumber, SmsVerificationPurpose purpose) {
        redisTemplate.delete(verifiedKey(phoneNumber, purpose));
    }

    private String codeKey(String phoneNumber) {
        return CODE_KEY_PREFIX + phoneNumber;
    }

    private String verifiedKey(String phoneNumber, SmsVerificationPurpose purpose) {
        return VERIFIED_KEY_PREFIX + purpose.name() + ":" + phoneNumber;
    }
}
