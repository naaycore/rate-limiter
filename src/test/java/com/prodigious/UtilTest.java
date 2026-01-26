package com.prodigious;

import com.prodigious.Configuration.domain.EndpointConfiguration;
import com.prodigious.Configuration.domain.LimitingAlgorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @Test
    void deserialize() {
        String json = """
                {
                  "path": "hello",
                  "bucketSize": 10,
                  "refillTokens": 2,
                  "refillInterval": {
                    "timeUnit": "SECONDS",
                    "value": 2
                  },
                  "algorithm": "TOKEN_BUCKET"
                }""";

        EndpointConfiguration config = Util.deserialize(json);
        assertEquals(LimitingAlgorithm.TOKEN_BUCKET, config.getAlgorithm());

    }
}