package com.prodigious.Configuration.domain;

import com.prodigious.ratelimiter.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterConfiguration {
    private static RateLimiterConfiguration instance;

    private final ConcurrentHashMap<String, RateLimiter> endpointRateLimiterMap;

    private RateLimiterConfiguration() {
        this.endpointRateLimiterMap = new ConcurrentHashMap<>();
    }

    public synchronized static RateLimiterConfiguration getInstance() {
        if (instance == null) {
            instance = new RateLimiterConfiguration();
        }
        return instance;
    }

    public ConcurrentHashMap<String, RateLimiter> getEndpointRateLimiterMap() {
        return endpointRateLimiterMap;
    }
}
