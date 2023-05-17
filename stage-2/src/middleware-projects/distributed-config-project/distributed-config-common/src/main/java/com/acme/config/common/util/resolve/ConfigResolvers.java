package com.acme.config.common.util.resolve;

import com.acme.config.common.ConfigKind;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class ConfigResolvers {

    private final static Map<ConfigKind, ConfigResolver> configResolvers = new HashMap<>();

    static {
        configResolvers.put(ConfigKind.JSON, new JsonConfigResolver());
    }

    public static Map<String, String> resolveConfigContent(ConfigKind configKind, String content) {
        ConfigResolver configResolver = configResolvers.get(configKind);
        return configResolver.apply(content);
    }

    public static boolean register(ConfigResolver configResolver) {
        Objects.requireNonNull(configResolver);
        ConfigKind kind = configResolver.supportKind();
        if (configResolvers.containsKey(kind))
            return false;

        configResolvers.putIfAbsent(kind, configResolver);
        return true;
    }

}
