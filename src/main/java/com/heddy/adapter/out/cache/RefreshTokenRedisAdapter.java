package com.heddy.adapter.out.cache;

import com.heddy.domain.account.port.out.RefreshTokenStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RefreshTokenRedisAdapter implements RefreshTokenStorePort {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final RedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3]) return 1 "
                    + "else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RefreshTokenRedisAdapter(
            StringRedisTemplate redisTemplate,
            @Value("${app.auth.refresh-token-seconds}") long refreshTokenSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(refreshTokenSeconds);
    }

    @Override
    public void save(Long accountId, String token) {
        redisTemplate.opsForValue().set(key(accountId), token, ttl);
    }

    @Override
    public void delete(Long accountId) {
        redisTemplate.delete(key(accountId));
    }

    @Override
    public boolean rotate(Long accountId, String expectedToken, String newToken) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(accountId)),
                expectedToken,
                newToken,
                Long.toString(ttl.toSeconds()));
        return result != null && result == 1L;
    }

    private String key(Long accountId) {
        return KEY_PREFIX + accountId;
    }
}
