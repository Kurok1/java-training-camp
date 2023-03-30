package indi.kurok1.curator.config.stream;

import indi.kurok1.curator.config.source.MapConfigUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义流数据解析器，默认每次返回都是一段json
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class DefaultJsonConfigAccumulator extends StreamConfigAccumulator {

    public static final String BEAN_NAME = "defaultJsonConfigAccumulator";


    public DefaultJsonConfigAccumulator(String name) {
        super(name);
    }

    @Override
    public PropertySource<?> update(String json, PropertySource<?> previous) throws IOException {
        Map<String, Object> data = MapConfigUtils.resolveByJson(json);
        if (previous == null) {
            return new MapPropertySource(name, data);
        } else {
            Map<String, Object> result = new ConcurrentHashMap<>();
            if (previous instanceof EnumerablePropertySource) {
                String[] keys = ((EnumerablePropertySource) previous).getPropertyNames();
                for (String key : keys)
                    result.put(key, previous.getProperty(key));
            }
            result.putAll(data);
            return new MapPropertySource(name, result);
        }

    }
}
