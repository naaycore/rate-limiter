package com.prodigious.Configuration.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public sealed class BucketEndpointConfiguration
        extends EndpointConfiguration
        permits LeakingBucketEndpointConfiguration,
                TokenBucketEndpointConfiguration {
    private int bucketSize;
}
