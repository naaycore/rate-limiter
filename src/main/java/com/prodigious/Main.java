package com.prodigious;

import com.prodigious.Zookeeper.ZkConfigManager;
import com.prodigious.Zookeeper.ZookeeperConfiguration;
import com.prodigious.httpProxy.RequestHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.prodigious.Util.registerEndpointsConfigurations;
import static com.prodigious.Util.retrieveEndpointConfigurations;

@Slf4j
public class Main {
    static void main() throws Exception {
        Map<String, String> configurations =
                retrieveEndpointConfigurations();

        ZkConfigManager zkConfigManager =
                ZookeeperConfiguration
                        .getInstance()
                        .getConfigManager();

        registerEndpointsConfigurations(configurations, zkConfigManager);


        zkConfigManager.start();

        RequestHandler requestHandler = new RequestHandler(8082, 10);
        Runtime.getRuntime().addShutdownHook(new Thread(requestHandler::stopHandler));
        requestHandler.startHandler();
    }
}
