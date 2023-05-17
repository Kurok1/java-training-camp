package com.acme.config.common;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public interface ConfigService {

    default String getProperty(String key) {
        return this.getProperty(key, null);
    }

    String getProperty(String key, String defaultValue);

    boolean registerWatcher(String configId, ConfigWatcher watcher);

    void deregisterWatcher(String configId, ConfigWatcher watcher);

}
