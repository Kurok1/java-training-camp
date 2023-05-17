package com.acme.config.client;

import com.acme.config.common.ConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class ConfigManagerProvider {

    private static final Map<ClientConfiguration, ConfigManager> managers = new ConcurrentHashMap<>();

    public static ConfigManager getConfigManager(ClientConfiguration configuration) {
        return managers.computeIfAbsent(configuration, ConfigClient::new);
    }

}
