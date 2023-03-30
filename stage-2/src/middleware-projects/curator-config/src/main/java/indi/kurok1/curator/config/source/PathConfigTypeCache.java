package indi.kurok1.curator.config.source;

import indi.kurok1.curator.config.ConfigType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class PathConfigTypeCache {

    private static final Map<String, ConfigType> configTypeCache = new ConcurrentHashMap<>();

    public static void setConfigType(String path, ConfigType configType) {
        configTypeCache.putIfAbsent(path, configType);
    }

    public static ConfigType get(String path) {
        return configTypeCache.get(path);
    }

}
