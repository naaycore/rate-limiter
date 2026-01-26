package com.prodigious.Configuration.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LimitingAlgorithm {
    TOKEN_BUCKET,
    LEAKING_BUCKET,
    FIXED_WINDOW_COUNTER,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER
}
