package com.acme.config.server.impl;

import com.acme.config.common.*;
import com.acme.config.common.util.FlattenedMapUtils;
import com.acme.config.common.util.MD5Utils;
import com.acme.config.common.util.resolve.ConfigResolvers;
import com.acme.config.server.watcher.SseEmitterServer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认实现{@link ConfigManager}
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Component
public class DefaultConfigManager implements ConfigManager, ConfigService {

    private final Map<String, ConfigEntry> configs = new ConcurrentHashMap<>();

    private final SseEmitterServer emitterServer = new SseEmitterServer();

    private final Map<String, Collection<ConfigWatcher>> configWatcherMap = new ConcurrentHashMap<>();

    @Override
    public void saveConfig(ConfigEntry configEntry) {
        final String configId = configEntry.getConfigId();
        LocalDateTime now = LocalDateTime.now();
        Objects.requireNonNull(configId, "config id cannot be null");
        //根据content生成map
        ConfigKind kind = ConfigKind.valueOf(configEntry.getConfigType());
        String md5 = MD5Utils.generateMD5(configEntry.getContent());
        configEntry.setContentMd5(md5);
        final Map<String, String> contentMap = ConfigResolvers.resolveConfigContent(kind, configEntry.getContent());
        configEntry.setContentMap(contentMap);
        configEntry.setUpdated(now);
        if (configs.containsKey(configId)) {
            //存在，覆盖，配置
            ConfigEntry oldConfig = getConfig(configId);

            final Map<String, String> oldMap = oldConfig.getContentMap();
            Collection<String> diffKeys = FlattenedMapUtils.getDiffKeys(contentMap, oldMap);
            //覆盖
            configEntry.setCreated(oldConfig.getCreated());
            this.configs.put(configId, configEntry);
            if (!CollectionUtils.isEmpty(diffKeys) && this.configWatcherMap.containsKey(configId)) {
                //存在差异，触发监听
                final Collection<ConfigWatcher.ChangedConfigEntry> entries = new HashSet<>();
                for (String key : diffKeys) {
                    String newValue = contentMap.get(key);
                    String oldValue = oldMap.get(key);
                    entries.add(new ConfigWatcher.ChangedConfigEntry(key, oldValue, newValue));
                }
                Collection<ConfigWatcher> watchers = this.configWatcherMap.get(configId);
                if (!CollectionUtils.isEmpty(watchers))
                    watchers.forEach(watcher -> watcher.onChange(configId, entries));
            }
        } else {
            //直接写入
            configEntry.setCreated(now);
            this.configs.put(configId, configEntry);

            //注册默认Watcher
            registerWatcher(configId, emitterServer);
        }
    }

    @Override
    public void deleteConfig(String configId) {
        this.configs.remove(configId);
    }

    @Override
    public ConfigService getConfigService() {
        return this;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConfigEntry getConfig(String configId) {
        return this.configs.get(configId);
    }

    @Override
    public boolean registerWatcher(String configId, ConfigWatcher watcher) {
        Objects.requireNonNull(watcher, "watcher cannot be null");
        if (!this.configs.containsKey(configId))
            return false;

        Collection<ConfigWatcher> configWatchers = this.configWatcherMap.computeIfAbsent(configId, id -> new ArrayList<>());
        return configWatchers.add(watcher);
    }

    @Override
    public void deregisterWatcher(String configId, ConfigWatcher watcher) {
        if (!this.configWatcherMap.containsKey(configId))
            return;

        Collection<ConfigWatcher> configWatchers = this.configWatcherMap.get(configId);
        configWatchers.remove(watcher);
    }
}
