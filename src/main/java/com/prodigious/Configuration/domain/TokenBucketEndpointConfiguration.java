package com.prodigious.Configuration.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public final class TokenBucketEndpointConfiguration
        extends BucketEndpointConfiguration {
    private int refillTokens;
    private Interval refillInterval;
    private LimitingAlgorithm algorithm;
}
