package com.prodigious.exception;

public class RateLimiterException extends Exception{
    public RateLimiterException(String msg){
        super(msg);
    }
}
