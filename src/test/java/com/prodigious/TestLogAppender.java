package com.prodigious;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class TestLogAppender extends AppenderBase<ILoggingEvent> {
    private final StringBuilder logs = new StringBuilder();

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        logs.append(iLoggingEvent.getFormattedMessage()).append(" ");
    }

    public String getLogs(){
        return logs.toString();
    }
}
