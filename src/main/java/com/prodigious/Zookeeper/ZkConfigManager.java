package com.prodigious.Zookeeper;

import com.prodigious.Configuration.domain.RateLimiterConfiguration;
import com.prodigious.Util;
import com.prodigious.ratelimiter.RateLimiter;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZkConfigManager implements Closeable {
    private final CuratorFramework client;
    private final String basePath;
    private final PathChildrenCache cache;

    private final ConcurrentHashMap<String, RateLimiter> config;

    private volatile boolean started = false;
    private final boolean clientIsManaged;

    public ZkConfigManager(
            String connectionString,
            String basePath,
            int sessionTimeoutMs,
            int connectionTimeoutMs
    ) {
        this(
                createDefaultClient(
                        connectionString,
                        sessionTimeoutMs,
                        connectionTimeoutMs
                ),
                basePath,
                true
        );
    }

    public ZkConfigManager(
            @NotNull(message = "Zookeeper client cannot be null") CuratorFramework client,
            @NotNull(message = "BasePath cannot be null") String basePath,
            boolean clientIsManaged
    ) {
        this.config = RateLimiterConfiguration
                .getInstance()
                .getEndpointRateLimiterMap();
        this.client = client;
        this.basePath = basePath;
        this.clientIsManaged = clientIsManaged;
        this.cache = new PathChildrenCache(client, basePath, true);
    }

    public Map<String, RateLimiter> getConfig() {
        return config;
    }

    public void create(String childPath, String val) throws Exception {
        String fullPath = genFullPath(childPath);

        synchronized (client) {
            client
                    .create()
                    .creatingParentsIfNeeded()
                    .forPath(
                            fullPath,
                            val.getBytes(StandardCharsets.UTF_8)
                    );
        }
    }

    public void createIfNotExistsElseModify(String childPath, String val)
            throws Exception {
        String fullPath = genFullPath(childPath);
        byte[] updatedValue = val.getBytes(StandardCharsets.UTF_8);


        for (int attempts = 0; attempts < 3; attempts++) {
            Stat stat = client.checkExists().forPath(fullPath);

            if (stat == null) {
                try {
                    create(childPath, val);
                    return;
                } catch (KeeperException.NodeExistsException e) {
                    log.warn(
                            "path created by a concurrent writer between check exists and create operations");
                }
            } else {
                byte[] current = client.getData().forPath(fullPath);
                if (Arrays.equals(current, updatedValue)) {
                    return;
                }
                try {
                    modifyValue(childPath, val);
                    return;
                } catch (KeeperException.NoNodeException e) {
                    log.warn(
                            "Unable to complete path modification of path {}",
                            fullPath
                    );
                }
            }
        }
        throw new IllegalStateException(
                "Failed to update zookeeper node due to concurrent writes: "
                        + fullPath);
    }

    public void modifyValue(@NotNull String key, String val)
            throws Exception {
        String fullPath = genFullPath(key);

        synchronized (client) {
            client.setData().forPath(
                    fullPath,
                    val.getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    public synchronized void start() throws Exception {
        if (started) {
            return;
        }
        ensureBasePath();
        loadInitialConfig();
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        started = true;
    }

    private String genFullPath(String childPath) {
        return basePath + "/" + childPath;
    }

    public void addListener(PathChildrenCacheListener listener) {
        cache.getListenable().addListener(listener);
    }

    private void ensureBasePath() throws Exception {
        if (client.checkExists().forPath(basePath) == null) {
            client.create().creatingParentsIfNeeded().forPath(basePath);
        }
    }

    private void loadInitialConfig() throws Exception {
        List<String> children = client.getChildren().forPath(basePath);
        for (String child : children) {
            String fullPath = basePath + "/" + child;
            byte[] data = client.getData().forPath(fullPath);
            config.put(
                    child,
                    Util.createRateLimiter(Util.deserialize(data))
            );
        }
    }

    private static CuratorFramework createDefaultClient(
            String connectionString,
            int sessionTimeoutMs,
            int connectionTimeoutMs
    ) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(
                1000,
                3
        );
        CuratorFramework client = CuratorFrameworkFactory
                .builder()
                .connectString(connectionString)
                .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connectionTimeoutMs)
                .retryPolicy(retryPolicy)
                .build();

        client.start();

        try {
            if (!client.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Failed to connection to Zookeeper within 30s");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while connected to Zookeeper",
                    exception
            );
        }
        return client;
    }

    @Override
    public void close() {
        try {
            cache.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (clientIsManaged) {
                client.close();
            }
            started = false;
        }
    }
}
