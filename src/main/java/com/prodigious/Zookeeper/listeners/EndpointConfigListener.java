package com.prodigious.Zookeeper.listeners;

import com.prodigious.Util;
import com.prodigious.ratelimiter.RateLimiter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.util.Map;

public class EndpointConfigListener implements PathChildrenCacheListener {
    private final Map<String, RateLimiter> endpointLimiterMap;
    private final String basePath;

    public EndpointConfigListener(
            Map<String, RateLimiter> endpointLimiter,
            String basePath
    ) {
        this.endpointLimiterMap = endpointLimiter;
        this.basePath = basePath;
    }

    @Override
    public void childEvent(
            CuratorFramework curatorFramework,
            PathChildrenCacheEvent event
    ) throws Exception {
        ChildData data = event.getData();
        if (data == null) {
            return;
        }

        handleEvent(event);
    }

    private void handleEvent(PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
            case CHILD_ADDED, CHILD_UPDATED -> endpointLimiterMap
                    .put(
                            extractKey(event.getData().getPath()),
                            Util.createRateLimiter(
                                    Util.deserialize(
                                            event.getData().getData()))
                    );
            case CHILD_REMOVED -> endpointLimiterMap
                    .remove(extractKey(event
                                               .getData()
                                               .getPath()));
            default -> throw new RuntimeException("Not implemented");
        }
    }

    private String extractKey(String fullPath) {
        if (!fullPath.startsWith(basePath)) {
            return fullPath;
        }

        String relative = fullPath.substring(basePath.length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return relative;
    }


}
