package indi.kurok1.curator.config.source;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class MapConfigUtilsTest {

    public static void main(String[] args) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("key1", "value");
        dataMap.put("key2", Map.of("a", "b"));
        dataMap.put("key3", List.of(Map.of("k1", "v1", "k2", "v2"), Map.of("k3", "v3")));
        dataMap.put("key4", Map.of("a", Map.of("b", "c")));
        MapConfigUtils.flattenedMap(result, dataMap, "");
        result.isEmpty();
    }

}
