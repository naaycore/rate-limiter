package com.prodigious;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodigious.Configuration.domain.EndpointConfiguration;
import com.prodigious.Redis.RedisTokenBucketRateLimiter;
import com.prodigious.Zookeeper.ZkConfigManager;
import com.prodigious.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Util {
    public static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        } catch (FileNotFoundException e) {
            log.error("File {} not found", path, e);
        } catch (IOException e) {
            log.error("Error reading file {}", path, e);
        }

        return sb.toString().trim();
    }

    public static Map<String, String> retrieveEndpointConfigurations()
            throws Exception {
        String parentDirectory = "src/main/resources/endpointConfigurations";

        Set<String> files = retrieveConfigFiles(parentDirectory);
        Map<String, String> map = new HashMap<>();

        for (String file : files) {
            String endpointConfigString =
                    readFile(parentDirectory + "/" + file);

            String[] part = file.split("\\.");
            if (part.length != 2) {
                throw new Exception("Malformed endpoint file name");
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
    ) throws Exception {
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
