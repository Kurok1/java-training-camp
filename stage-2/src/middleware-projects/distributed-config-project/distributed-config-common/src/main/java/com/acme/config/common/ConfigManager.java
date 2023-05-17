package com.acme.config.common;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public interface ConfigManager {

    void saveConfig(ConfigEntry configEntry);

    ConfigEntry getConfig(String configId);

    void deleteConfig(String configId);

    ConfigService getConfigService();

}
