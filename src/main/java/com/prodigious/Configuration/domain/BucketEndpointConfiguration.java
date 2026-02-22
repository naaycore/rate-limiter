package com.prodigious.Configuration.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BucketEndpointConfiguration extends EndpointConfiguration {
    private int bucketSize;
}
