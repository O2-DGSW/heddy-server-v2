package com.heddy.adapter.out.cache;

import com.heddy.domain.account.model.SocialProvider;
import com.heddy.domain.account.port.out.PendingSocialLoginStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PendingSocialLoginRedisAdapter implements PendingSocialLoginStorePort {

    private static final String KEY_PREFIX = "auth:social:pending:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String pendingToken, SocialProvider provider, String providerId) {
        redisTemplate.opsForValue().set(
                key(pendingToken), provider.name() + ":" + providerId, TTL);
    }

    @Override
    public Optional<PendingSocialLogin> find(String pendingToken) {
        String value = redisTemplate.opsForValue().get(key(pendingToken));
        if (value == null) {
            return Optional.empty();
        }
        int separator = value.indexOf(':');
        if (separator < 1 || separator == value.length() - 1) {
            redisTemplate.delete(key(pendingToken));
            return Optional.empty();
        }
        try {
            return Optional.of(new RedisPendingSocialLogin(
                    SocialProvider.valueOf(value.substring(0, separator)),
                    value.substring(separator + 1)));
        } catch (IllegalArgumentException exception) {
            redisTemplate.delete(key(pendingToken));
            return Optional.empty();
        }
    }

    @Override
    public void delete(String pendingToken) {
        redisTemplate.delete(key(pendingToken));
    }

    private String key(String pendingToken) {
        return KEY_PREFIX + pendingToken;
    }

    private record RedisPendingSocialLogin(
            SocialProvider provider,
            String providerId
    ) implements PendingSocialLogin {
    }
}
