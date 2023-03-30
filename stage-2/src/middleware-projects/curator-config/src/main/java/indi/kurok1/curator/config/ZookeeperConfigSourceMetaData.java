package indi.kurok1.curator.config;

import indi.kurok1.curator.config.stream.DefaultJsonConfigAccumulator;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class ZookeeperConfigSourceMetaData {

    private final String nodePath;

    private final ConfigType configType;

    private final boolean watchChange;

    private final Class<?> streamConfigAccumulatorClass;

    private ZookeeperConfigSourceMetaData(String nodePath, ConfigType configType, boolean watchChange, Class<?> streamConfigAccumulatorClass) {
        this.nodePath = nodePath;
        this.configType = configType;
        this.watchChange = watchChange;
        this.streamConfigAccumulatorClass = streamConfigAccumulatorClass;
    }

    public String getNodePath() {
        return nodePath;
    }

    public ConfigType getConfigType() {
        return configType;
    }

    public boolean isWatchChange() {
        return watchChange;
    }

    public Class<?> getStreamConfigAccumulatorClass() {
        return streamConfigAccumulatorClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZookeeperConfigSourceMetaData that = (ZookeeperConfigSourceMetaData) o;

        if (!nodePath.equals(that.nodePath)) return false;
        return configType == that.configType;
    }

    @Override
    public int hashCode() {
        int result = nodePath.hashCode();
        result = 31 * result + configType.hashCode();
        return result;
    }

    public static ZookeeperConfigSourceMetaData byMap(Map<String, Object> map) {
        if (CollectionUtils.isEmpty(map))
            return null;


        String nodeParentPath = (String) map.get("path");
        ConfigType configType = (ConfigType) map.get("configType");
        Boolean watchChange = (Boolean) map.getOrDefault("watchChange", Boolean.TRUE);
        Class<?> streamConfigAccumulatorClass = (Class<?>) map.getOrDefault("streamConfigAccumulator", DefaultJsonConfigAccumulator.class);
        return new ZookeeperConfigSourceMetaData(nodeParentPath, configType, watchChange, streamConfigAccumulatorClass);
    }

}
