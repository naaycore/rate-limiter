package com.prodigious.ratelimiter;

public class RateLimiterException extends Exception{
    public RateLimiterException(String msg){
        super(msg);
    }
}
