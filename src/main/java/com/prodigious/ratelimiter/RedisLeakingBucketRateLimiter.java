package com.prodigious.ratelimiter;

import com.prodigious.Configuration.domain.LeakingBucketEndpointConfiguration;

public final class RedisLeakingBucketRateLimiter implements RateLimiter {

    public RedisLeakingBucketRateLimiter(
            LeakingBucketEndpointConfiguration configuration
    ) {

    }

    @Override
    public boolean allow(String identity, String routPath) {
        return false;
    }
}
