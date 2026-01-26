package com.prodigious.Zookeeper;

import com.prodigious.Zookeeper.listeners.EndpointConfigListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZookeeperConfiguration {
    private static ZookeeperConfiguration instance;
    private final ZkConfigManager configManager;

    private ZookeeperConfiguration() {
//        ConcurrentHashMap<String, RateLimiter> configurationMap =
//                RateLimiterConfiguration
//                        .getInstance()
//                        .getEndpointRateLimiterMap();

        String connectionString = "localhost:2181";
        String basePath = "/prodigious/rate-limiter";

        configManager = new ZkConfigManager(
                connectionString,
                basePath,
                60_000,
                15_000
        );

        try {
            EndpointConfigListener listener = new EndpointConfigListener(
                    configManager.getConfig(),
                    basePath
            );
            configManager.addListener(listener);
        } catch (Exception e) {
            log.error("Error starting zookeeper config", e);
        }


    }

    public synchronized static ZookeeperConfiguration getInstance() {
        if (instance == null) {
            instance = new ZookeeperConfiguration();
        }

        return instance;
    }

    public ZkConfigManager getConfigManager() {
        return configManager;
    }
}
