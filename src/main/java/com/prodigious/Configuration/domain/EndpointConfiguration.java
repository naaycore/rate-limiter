package com.prodigious.Configuration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EndpointConfiguration {
    private String path;
    private int bucketSize;
    private int refillTokens;
    private Interval refillInterval;
    private LimitingAlgorithm algorithm;
}
