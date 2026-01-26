package com.prodigious.Redis;

import com.prodigious.ratelimiter.RateLimiter;
import com.prodigious.Util;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.RedisClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class RedisTokenBucketRateLimiter implements RateLimiter {
    private final RedisClient redisClient;
    private final String scriptSha;

    private final long capacity;
    private final long refillTokens;
    private final long refillIntervalMs;

    public RedisTokenBucketRateLimiter(
            String luaScriptPath,
            long capacity,
            long refillTokens,
            long refillIntervalMs
    ) {
        this.redisClient = RedisConfiguration.getInstance().getClient();
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.scriptSha = redisClient.scriptLoad(Util.readFile(luaScriptPath));
    }

    @Override
    public boolean allow(String identity, String routePath) {
        return allow(identity, routePath, 1);
    }

    public boolean allow(String identity, String routePath, long cost) {
        long nowMs = Instant.now().toEpochMilli();
        String redisKey = "rate:" + identity + ":" + routePath;

        List<String> keys = Collections.singletonList(redisKey);
        List<String> args = Arrays.asList(
                String.valueOf(capacity),
                String.valueOf(refillTokens),
                String.valueOf(refillIntervalMs),
                String.valueOf(cost),
                String.valueOf(nowMs)
        );

        Object result = redisClient.evalsha(scriptSha, keys, args);

        List<?> list = (List<?>) result;

        Long allowed = toLong(list.getFirst());

        log.info("remaining tokens: {}", list.get(1));
        log.info("last refill: {}", list.get(2));
        return allowed != null && allowed == 1L;
    }

    private static Long toLong(Object o){
        return switch (o) {
            case null -> null;
            case Long l -> l;
            case Integer i -> i.longValue();
            case String s -> Long.parseLong(s);
            default -> throw new IllegalArgumentException("Unexpected Lua return type: " + o.getClass());
        };
    }
}
