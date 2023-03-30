package indi.kurok1.curator.config.source;

import indi.kurok1.curator.config.ConfigType;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.env.*;

import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class ZookeeperConfigUtils {

    /**
     * 提取配置项，并且保持，同时反馈差异key
     * @param environment spring environment
     * @param client zookeeper client
     * @param path path
     * @param configType 配置类型
     * @return 更新时变化key列表，新增返回空
     * @throws Exception 可能出现的异常
     */
    public static Collection<String> retrieveConfigAndSave(ConfigurableEnvironment environment, CuratorFramework client, String path, ConfigType configType) throws Exception {
        //读取数据
        byte[] data = client.getData().forPath(path);

        MutablePropertySources propertySources = environment.getPropertySources();
        PropertySource<?> old = propertySources.get(path);
        PropertySource<?> propertySource = null;
        switch (configType) {
            case TEXT:propertySource = new ZookeeperTextPropertySource(path, new String(data, UTF_8));break;
            case JSON:{
                Map<String, Object> result = MapConfigUtils.resolveByJson(data);
                propertySource = new ZookeeperMapPropertySource(path, result, ConfigType.JSON);
            }break;
            case XML:{
                Map<String, Object> result = MapConfigUtils.resolveByXml(data);
                propertySource = new ZookeeperMapPropertySource(path, result, ConfigType.XML);
            }break;
        }
        if (propertySource != null) {
            if (old == null) {//不存在老配置，新增
                propertySources.addFirst(propertySource);
            } else propertySources.replace(path, propertySource); //存在老配置，替换
        }
        PathConfigTypeCache.setConfigType(path, configType);


        //比较差异
        return compareDiff(propertySource, environment);

    }

    public static Collection<String> compareDiff(PropertySource<?> newConfig, Environment environment) {

        if (newConfig instanceof EnumerablePropertySource) {
            Set<String> diffKeys = new HashSet<>();
            String[] keys = ((EnumerablePropertySource) newConfig).getPropertyNames();
            for (String key : keys) {
                Object oldValue = environment.getProperty(key);
                Object newValue = newConfig.getProperty(key);
                if (newValue == null && oldValue == null)//同时为null
                    continue;

                if (newValue.equals(oldValue))
                    continue;

                diffKeys.add(key);
            }
            return diffKeys;
        }

        return Collections.emptyList();
    }

    public static Collection<String> retrieveConfigAndSave(ConfigurableEnvironment environment, CuratorFramework client, String path) throws Exception {
        ConfigType configType = PathConfigTypeCache.get(path);
        if (configType != null)
            return retrieveConfigAndSave(environment, client, path, configType);

        return Collections.emptyList();
    }

}
