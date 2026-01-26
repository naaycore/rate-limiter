package com.prodigious.httpProxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpProxyUtilTest {
    @Test
    void testExtractSessionId_noColumn() {
        String line = "CookieSESSIONID=abcd1234";
        Exception exception =
                assertThrows(
                        HttpProxyException.class,
                        () -> HttpProxyUtil.extractSessionId(line)
                );

        assertEquals("Malformed Cookie", exception.getMessage());
    }

    @Test
    void testExtractSessionId_noEqual() {
        String line = "Cookie:SESSIONIDabcd1234";
        Exception exception =
                assertThrows(
                        HttpProxyException.class,
                        () -> HttpProxyUtil.extractSessionId(line)
                );

        assertEquals("Malformed session cookie", exception.getMessage());
    }

    @Test
    void testExtractSessionId() throws HttpProxyException {
        String line = "Cookie:SESSIONID=abcd1234";

        String sessionId = HttpProxyUtil.extractSessionId(line);

        assertEquals("abcd1234", sessionId);
    }
}