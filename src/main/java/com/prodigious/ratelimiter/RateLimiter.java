package com.prodigious.ratelimiter;

public sealed interface RateLimiter permits RedisLeakingBucketRateLimiter,
                                            RedisTokenBucketRateLimiter {
    boolean allow(String identity, String routPath);
}
