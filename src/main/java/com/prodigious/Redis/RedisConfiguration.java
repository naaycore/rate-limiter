package com.prodigious.Redis;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.RedisClient;

public class RedisConfiguration {
    private final RedisClient client;

    private static RedisConfiguration instance;

    private RedisConfiguration() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(10);
        client = RedisClient
                .builder()
                .hostAndPort("localhost", 6379)
                .poolConfig(poolConfig)
                .build();
    }

    public synchronized static RedisConfiguration getInstance() {
        if (instance == null) {
            instance = new RedisConfiguration();
        }
        return instance;
    }

    public RedisClient getClient(){
        return client;
    }
}
