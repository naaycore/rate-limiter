package com.prodigious_o.rateLimiter;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterUtilTest {
    @Test
    public void testReadLuaScript() throws IOException {
        String textContent = RateLimiterUtil.readLuaScript("src/test/resources/test.txt");
        assertEquals("This is a test file", textContent);
    }
}