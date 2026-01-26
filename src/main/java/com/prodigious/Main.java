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
    static void main(String... args) throws Exception {
        if(args.length < 1){
            throw new Exception("Endpoint configuration not provided");
        }
        Map<String, String> configurations =
                retrieveEndpointConfigurations(args[0]);

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
