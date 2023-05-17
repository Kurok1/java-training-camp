package com.acme.config.client;

import com.acme.config.common.ConfigEntry;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地配置管理集
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class ConfigEntrySet {

    private final Set<ConfigEntry> configEntries = new TreeSet<>(ConfigEntry::compareTo);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();


    public void addConfigEntry(ConfigEntry configEntry) {
        writeLock.lock();
        this.configEntries.add(configEntry);
        writeLock.unlock();
    }

    public ConfigEntry getConfigEntryById(String configId) {
        readLock.lock();
        try {
            for (ConfigEntry configEntry : this.configEntries)
                if (configEntry.getConfigId().equals(configId))
                    return configEntry;

            return null;
        } finally {
            readLock.unlock();
        }
    }

    public String getProperty(String key) {
        readLock.lock();
        try {
            for (ConfigEntry configEntry : configEntries) {
                Map<String, String> map = configEntry.getContentMap();
                if (map.containsKey(key))
                    return map.get(key);
            }
            return null;
        } finally {
            readLock.lock();
        }
    }

}
