package com.prodigious.Configuration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public sealed class EndpointConfiguration
        permits BucketEndpointConfiguration {
    private String path;
    private LimitingAlgorithm algorithm;
}
