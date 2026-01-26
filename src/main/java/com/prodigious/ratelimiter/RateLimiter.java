package com.prodigious.ratelimiter;

public interface RateLimiter {
    boolean allow(String identity, String routPath);
}
