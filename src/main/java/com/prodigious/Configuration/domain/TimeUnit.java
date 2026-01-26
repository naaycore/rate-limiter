package com.prodigious.Configuration.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TimeUnit {
    MILLISECONDS("ms", 1L),
    SECONDS("s", 1000L),
    MINUTES("m", 60_000L),
    HOURS("h", 60 * 60_000L),
    DAYS("d", 24 * 60 * 60_000L);

    private String unit;
    private final long ms;

    private TimeUnit(String unit, long ms){
        this.unit = unit;
        this.ms = ms;
    }

    public String getUnit(){
        return unit;
    }

    public long getMs(){
        return ms;
    }
}
