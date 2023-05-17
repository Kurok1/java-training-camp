package com.acme.config.client;

import com.acme.config.common.ConfigEntry;
import com.acme.config.common.ConfigService;
import com.acme.config.common.ConfigWatcher;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地配置服务实现
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class LocalConfigService implements ConfigService, AutoCloseable {


    private final Map<String, ConfigEntry> configEntryMap = new ConcurrentHashMap<>();

    private final ConfigWatcherDispatcher dispatcher = new ConfigWatcherDispatcher();

    private final ConfigEntrySet configEntrySet;


    private final Thread listenThread;

    public LocalConfigService(ClientConfiguration clientConfiguration, ConfigEntrySet configEntrySet, RestTemplate restTemplate) {
        this.configEntrySet = configEntrySet;
        this.listenThread = new Thread(new ConfigListenTask(clientConfiguration, dispatcher, restTemplate));
        listenThread.setDaemon(true);
        listenThread.setName("config-client-listener");
        listenThread.start();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = this.configEntrySet.getProperty(key);
        if (value == null)
            return defaultValue;
        return value;
    }

    @Override
    public boolean registerWatcher(String configId, ConfigWatcher watcher) {
        return dispatcher.registerWatcher(configId, watcher);
    }

    @Override
    public void deregisterWatcher(String configId, ConfigWatcher watcher) {
        dispatcher.deregisterWatcher(configId, watcher);
    }

    @Override
    public void close() throws Exception {
        this.listenThread.interrupt();
    }
}
