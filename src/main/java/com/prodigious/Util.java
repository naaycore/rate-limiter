package com.prodigious;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodigious.Configuration.domain.EndpointConfiguration;
import com.prodigious.Redis.RedisTokenBucketRateLimiter;
import com.prodigious.Zookeeper.ZkConfigManager;
import com.prodigious.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Util {
    public static String readFile(String path) {
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading file {}", path, e);
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> retrieveEndpointConfigurations(
            String endpointConfigurationDir
    ) {
        Set<String> files = retrieveConfigFiles(endpointConfigurationDir);
        Map<String, String> map = new HashMap<>();

        for (String file : files) {
            String endpointConfigString = readFile(endpointConfigurationDir + "/" + file);

            String[] part = file.split("\\.");

            if (part.length != 2 || !part[1].equals("json")) {
                log.warn("File {} is not a json file, ignoring", file);
                continue;
            }
            map.put(part[0], endpointConfigString);
        }

        return map;
    }

    public static EndpointConfiguration deserialize(byte[] bytes) {
        String json = bytesToString(bytes);
        return deserialize(json);
    }

    private static String bytesToString(byte[] data) {
        return data == null ? null : new String(data, StandardCharsets.UTF_8);
    }

    public static EndpointConfiguration deserialize(String json) {
        EndpointConfiguration configuration = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            configuration = mapper.readValue(json, EndpointConfiguration.class);
        } catch (JsonProcessingException e) {
            log.error("Error creating Object from string", e);
        }
        return configuration;
    }

    public static RateLimiter createRateLimiter(
            EndpointConfiguration configuration
    ) {
        return switch (configuration.getAlgorithm()) {
            case TOKEN_BUCKET -> new RedisTokenBucketRateLimiter(
                    "ratelimiter.lua",
                    configuration.getBucketSize(),
                    configuration.getRefillTokens(),
                    configuration
                            .getRefillInterval()
                            .getTimeUnit()
                            .getMs() * configuration
                            .getRefillInterval()
                            .getValue()
            );

            default -> throw new RuntimeException("Not yet implemented");
        };
    }

    static void registerEndpointsConfigurations(
            Map<String, String> configurations,
            ZkConfigManager configManager
    ) throws Exception {
        for (String key : configurations.keySet()) {
            String configuration = configurations.get(key);
            configManager.createIfNotExistsElseModify(key, configuration);
        }
    }

    private static Set<String> retrieveConfigFiles(String dir) {
        return Stream
                .of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }
}
