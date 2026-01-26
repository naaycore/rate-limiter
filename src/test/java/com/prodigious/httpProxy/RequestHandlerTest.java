package com.prodigious.httpProxy;

import com.prodigious.Configuration.domain.RateLimiterConfiguration;
import com.prodigious.ratelimiter.RateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestHandlerTest {
    private RequestHandler handler;

    @BeforeEach
    void setup() throws IOException {
        RateLimiterConfiguration.getInstance()
                                .getEndpointRateLimiterMap()
                                .clear();
        handler = new RequestHandler(0, 1);
    }

    @AfterEach
    void tearDown() {
        handler.stopHandler();
    }

    @Test
    void rateLimitPipe_allowsAndWritesRequestLines() throws Exception {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        RateLimiterConfiguration.getInstance()
                                .getEndpointRateLimiterMap()
                                .put("test", rateLimiter);
        when(rateLimiter.allow("abc", "test")).thenReturn(true);

        String request = "GET /test?foo=1 HTTP/1.1\r\n"
                + "Cookie: SESSIONID=abc\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        invokeRateLimitPipe(reader, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertEquals(request, output);
        verify(rateLimiter).allow("abc", "test");
    }

    @Test
    void rateLimitPipe_blocksWhenRateLimitExceeded() throws Exception {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        RateLimiterConfiguration.getInstance()
                                .getEndpointRateLimiterMap()
                                .put("test", rateLimiter);
        when(rateLimiter.allow("abc", "test")).thenReturn(false);

        String request = "GET /test HTTP/1.1\r\n"
                + "Cookie: SESSIONID=abc\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        invokeRateLimitPipe(reader, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertEquals("GET /test HTTP/1.1\r\n", output);
        verify(rateLimiter).allow("abc", "test");
    }

    @Test
    void rateLimitPipe_rejectsMalformedRequestLine() throws Exception {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        RateLimiterConfiguration.getInstance()
                                .getEndpointRateLimiterMap()
                                .put("test", rateLimiter);

        String request = "BADLINE\r\n"
                + "Cookie: SESSIONID=abc\r\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        invokeRateLimitPipe(reader, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertEquals("", output);
        verifyNoInteractions(rateLimiter);
    }

    private void invokeRateLimitPipe(
            BufferedReader reader,
            OutputStream outputStream
    ) throws Exception {
        Method method = RequestHandler.class.getDeclaredMethod(
                "rateLimitPipe",
                BufferedReader.class,
                OutputStream.class
        );
        method.setAccessible(true);
        method.invoke(handler, reader, outputStream);
    }
}
