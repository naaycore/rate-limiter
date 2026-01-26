package com.prodigious;

import ch.qos.logback.classic.Logger;
import com.prodigious.Configuration.domain.EndpointConfiguration;
import com.prodigious.Configuration.domain.LimitingAlgorithm;
import com.prodigious.Configuration.domain.TimeUnit;
import com.prodigious.Redis.RedisTokenBucketRateLimiter;
import com.prodigious.Zookeeper.ZkConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.prodigious.Util.createRateLimiter;
import static com.prodigious.Util.deserialize;
import static com.prodigious.Util.retrieveEndpointConfigurations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class UtilTest {
    private TestLogAppender logAppender;

    @BeforeEach
    public void setup() {
        logAppender = new TestLogAppender();
        logAppender.start();
        Logger log = (Logger) LoggerFactory.getLogger(Util.class);
        log.addAppender(logAppender);
    }

    @Test
    void testReadFile_createsStringOfFilePathContent() {
        String path = "src/test/resources/test.txt";
        String content = Util.readFile(path);
        assertEquals("This is a test file\nwith a carriage return", content);
    }

    @Test
    void testReadFile_FileDoesNotExist() {
        String path = "src/test/resources/test_ghost.txt";
        Exception exception = assertThrows(
                RuntimeException.class, () -> Util.readFile(path)

        );

        String expectedMessage =
                "java.nio.file.NoSuchFileException: src/test/resources/test_ghost.txt";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testRetrieveEndpointConfigurations_nullInput() {
        String path = null;

        Exception exception = assertThrows(
                NullPointerException.class,
                () -> retrieveEndpointConfigurations(path)
        );

        assertNull(exception.getMessage());
    }

    @Test
    void testRetrieveEndpointConfiguration_noFilesInDirectory() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/emptyDirectory");
        assertEquals(0, map.size());
    }

    @Test
    void testRetrieveEndpointConfiguration_ignoreNonJsonFiles() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/nonJson");
        String logs = logAppender.getLogs();

        assertTrue(logs.contains("File test.jsn is not a json file, ignoring"));
        assertEquals(0, map.size());
    }

    @Test
    void testRetrieveEndpointConfiguration_noFileExtension() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/noExtension");
        String logs = logAppender.getLogs();

        assertTrue(logs.contains("File file is not a json file, ignoring"));
        assertEquals(0, map.size());
    }

    @Test
    void testRetrieveEndpointConfiguration_folderWithJsonFiles() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/jsonFile");

        assertEquals(1, map.size());
    }

    @Test
    void testDeserialize_deserializeByteArray() {
        String s = """
                {
                    "path": "hello",
                    "bucketSize": 10,
                    "refillTokens": 2,
                    "refillInterval": {
                        "timeUnit": "MINUTES",
                        "value": 2
                    },
                    "algorithm": "TOKEN_BUCKET"
                }
                """;
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        EndpointConfiguration config = deserialize(bytes);

        assertNotNull(config);
        assertEquals("hello", config.getPath());
        assertEquals(10, config.getBucketSize());
        assertEquals(2, config.getRefillTokens());
        assertEquals(
                TimeUnit.MINUTES,
                config.getRefillInterval().getTimeUnit()
        );
        assertEquals(2, config.getRefillInterval().getValue());
        assertEquals(LimitingAlgorithm.TOKEN_BUCKET, config.getAlgorithm());
    }

    @Test
    void testDeserialize_deserializeNull() {
        byte[] bytes = null;
        Exception exception = assertThrows(
                NullPointerException.class, () -> deserialize(bytes));

        assertEquals(
                "Cannot read the array length because \"bytes\" is null",
                exception.getMessage()
        );
    }

    @Test
    void testDeserialize_deserializeString() {
        String s = """
                {
                    "path": "hello",
                    "bucketSize": 10,
                    "refillTokens": 2,
                    "refillInterval": {
                        "timeUnit": "MINUTES",
                        "value": 2
                    },
                    "algorithm": "TOKEN_BUCKET"
                }
                """;

        EndpointConfiguration config = deserialize(s);

        assertNotNull(config);
        assertEquals("hello", config.getPath());
        assertEquals(10, config.getBucketSize());
        assertEquals(2, config.getRefillTokens());
        assertEquals(
                TimeUnit.MINUTES,
                config.getRefillInterval().getTimeUnit()
        );
        assertEquals(2, config.getRefillInterval().getValue());
        assertEquals(LimitingAlgorithm.TOKEN_BUCKET, config.getAlgorithm());
    }

    @Test
    void testDeserialize_MalformedJson() {
        String s = """
                {
                    "path": "hello",
                    "bucketSize": 10,
                    "refillTokens": 2,
                    "refillInterval": {
                        "timeUnit": "MINUTES",
                        "value": 2
                    },
                    "algorithm": "TOKEN_BUCKET"
                
                """;

        Exception exception = assertThrows(
                RuntimeException.class,
                () -> deserialize(s)
        );

        assertTrue(exception
                           .getMessage().contains("Unexpected end-of-input: "));
    }

    @Test
    void testCreateRateLimiter() {
        String s = """
                {
                    "path": "hello",
                    "bucketSize": 10,
                    "refillTokens": 2,
                    "refillInterval": {
                        "timeUnit": "MINUTES",
                        "value": 2
                    },
                    "algorithm": "TOKEN_BUCKET"}
                
                """;
        EndpointConfiguration config = deserialize(s);

        Util.createRateLimiter(config);
    }

    @Test
    void testCreateRateLimiter_unimplementedAlgorithms() {
        String s = """
                {
                    "path": "hello",
                    "bucketSize": 10,
                    "refillTokens": 2,
                    "refillInterval": {
                        "timeUnit": "MINUTES",
                        "value": 2
                    },
                    "algorithm": "LEAKING_BUCKET"
                }
                """;

        EndpointConfiguration config = deserialize(s);

        Exception exception = assertThrows(
                RuntimeException.class,
                () -> createRateLimiter(config)
        );

        assertEquals("Not yet implemented", exception.getMessage());
    }


    @Test
    void registerEndpointConfigurations() throws Exception {
        ZkConfigManager configManager = Mockito.mock(ZkConfigManager.class);

        Mockito.doNothing().when(configManager).createIfNotExistsElseModify(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any()
        );

        String s = """
                {
                    "path": "hello",
                    "bucketSize": 10,
                    "refillTokens": 2,
                    "refillInterval": {
                        "timeUnit": "MINUTES",
                        "value": 2
                    },
                    "algorithm": "LEAKING_BUCKET"
                }
                """;
        Map<String, String> map = Map.of("hi", s);
        Util.registerEndpointsConfigurations(
                map,
                configManager
        );

        Mockito
                .verify(configManager, Mockito.times(1))
                .createIfNotExistsElseModify("hi", s);
    }
}