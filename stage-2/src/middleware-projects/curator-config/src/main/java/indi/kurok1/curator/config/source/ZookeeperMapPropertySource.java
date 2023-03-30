package indi.kurok1.curator.config.source;

import indi.kurok1.curator.config.ConfigType;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ZookeeperMapPropertySource extends MapPropertySource {

    protected final ConfigType configType;

    public ZookeeperMapPropertySource(String name, Map<String, Object> source, ConfigType configType) {
        super(name, source);
        this.configType = configType;
    }

    public ConfigType getConfigType() {
        return configType;
    }
}
