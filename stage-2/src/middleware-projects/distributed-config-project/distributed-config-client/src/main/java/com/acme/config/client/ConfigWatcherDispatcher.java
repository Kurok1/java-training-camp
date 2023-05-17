package com.acme.config.client;

import com.acme.config.common.ConfigWatcher;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置变化分发
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class ConfigWatcherDispatcher implements ConfigWatcher {

    private final Map<String, Collection<ConfigWatcher>> watchers = new ConcurrentHashMap<>();

    @Override
    public void onChange(String configId, Collection<ChangedConfigEntry> changedConfigs) {
        Collection<ConfigWatcher> configWatchers = this.watchers.get(configId);
        if (CollectionUtils.isEmpty(configWatchers))
            return;

        for (ConfigWatcher configWatcher : configWatchers)
            configWatcher.onChange(configId, changedConfigs);
    }

    public boolean registerWatcher(String configId, ConfigWatcher watcher) {
        Objects.requireNonNull(watcher, "watcher cannot be null");

        Collection<ConfigWatcher> configWatchers = this.watchers.computeIfAbsent(configId, id -> new HashSet<>());
        return configWatchers.add(watcher);
    }

    public void deregisterWatcher(String configId, ConfigWatcher watcher) {
        if (!this.watchers.containsKey(configId))
            return;

        Collection<ConfigWatcher> configWatchers = this.watchers.get(configId);
        configWatchers.remove(watcher);
    }
}
