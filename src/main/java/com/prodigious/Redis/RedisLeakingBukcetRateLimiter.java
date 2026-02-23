package com.prodigious.Redis;

import com.prodigious.Configuration.domain.LeakingBucketEndpointConfiguration;
import com.prodigious.ratelimiter.RateLimiter;

public class RedisLeakingBukcetRateLimiter implements RateLimiter {

    public RedisLeakingBukcetRateLimiter(
            LeakingBucketEndpointConfiguration configuration
    ) {

    }

    @Override
    public boolean allow(String identity, String routPath) {
        return false;
    }
}
