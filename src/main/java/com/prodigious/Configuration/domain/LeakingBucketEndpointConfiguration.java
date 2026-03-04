package com.prodigious.Configuration.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public final class LeakingBucketEndpointConfiguration
        extends BucketEndpointConfiguration {
    private Interval processingRate;
}
