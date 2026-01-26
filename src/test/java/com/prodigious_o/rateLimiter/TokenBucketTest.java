package com.prodigious_o.rateLimiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

//@ExtendWith(MockitoExtension.class)
class TokenBucketTest {
//    @Mock
//    TokenBucket tokenBucket;
//    @Test
    public void acquireToken_startWithFullTokens() {
        TokenBucket tokenBucket = new TokenBucket(
                5,
                Duration.of(1, ChronoUnit.MINUTES),
                2,
                true);

        assertEquals(1,tokenBucket.tryAcquire(1));
    }

//    @Test
    public void acquireToken_startWithNoTokens(){

        TokenBucket tokenBucket = new TokenBucket(
                5,
                Duration.of(1, ChronoUnit.MINUTES),
                2,
                false
        );
        assertEquals(0, tokenBucket.tryAcquire(1));
    }

//    @Test
    public void tryAcquire_MultipleValidRequests(){
        TokenBucket tokenBucket = new TokenBucket(
                5,
                Duration.of(1, ChronoUnit.MINUTES),
                2,
                true
        );
        assertEquals(1,tokenBucket.tryAcquire(1));
        assertEquals(2, tokenBucket.tryAcquire(2));
        assertEquals(2, tokenBucket.tryAcquire(2));
        assertEquals(0, tokenBucket.getToken());
    }

//    @Test
    public void tryAcquire_toManyRequests(){
        TokenBucket tokenBucket = new TokenBucket(
                5,
                Duration.of(1, ChronoUnit.MINUTES),
                2,
                true
        );

        assertEquals(5, tokenBucket.tryAcquire(5));
        assertEquals(0, tokenBucket.tryAcquire(1));
    }

//    @Test
    public void tryAcquire_Refill() throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(
                5,
                Duration.of(1, ChronoUnit.MINUTES),
                2,
                true
        );

        assertEquals(5, tokenBucket.tryAcquire(5));
        assertEquals(0, tokenBucket.tryAcquire(1));
        Thread.sleep(60000);
        assertEquals(2, tokenBucket.tryAcquire(2));
    }

}