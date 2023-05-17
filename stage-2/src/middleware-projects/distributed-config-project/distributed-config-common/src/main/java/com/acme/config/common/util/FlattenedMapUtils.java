package com.acme.config.common.util;

import java.util.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class FlattenedMapUtils {


    /**
     * 比较两个map版本的差异，以newMap为主
     * @param newMap
     * @param oldMap
     * @return
     */
    public static Collection<String> getDiffKeys(Map<String, String> newMap, Map<String, String> oldMap) {
        Set<String> diffKeys = new HashSet<>();
        Set<String> newKeys = newMap.keySet();
        Set<String> oldKeys = new HashSet<>();
        oldKeys.addAll(oldMap.keySet());
        for (String key : newKeys) {
            String oldValue = oldMap.get(key);
            String newValue = newMap.get(key);
            oldKeys.remove(key);
            if (newValue == null && oldValue == null)//同时为null
                continue;

            if (newValue.equals(oldValue))
                continue;

            diffKeys.add(key);

        }
        diffKeys.addAll(oldKeys);
        return diffKeys;
    }

    /**
     * copy by com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config:AbstractPropertySourceLoader
     * @param result
     * @param dataMap
     * @param parentKey
     */
    public static void flattenedMap(Map<String, String> result, Map<String, Object> dataMap,
                                    String parentKey) {
        if (dataMap == null || dataMap.isEmpty()) {
            return;
        }
        Set<Map.Entry<String, Object>> entries = dataMap.entrySet();
        for (Iterator<Map.Entry<String, Object>> iterator = entries.iterator(); iterator
                .hasNext();) {
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            Object value = entry.getValue();

            String fullKey = (parentKey == null || parentKey.isEmpty()) ? key : key.startsWith("[")
                    ? parentKey.concat(key) : parentKey.concat(".").concat(key);

            if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                flattenedMap(result, map, fullKey);
                continue;
            }
            else if (value instanceof Collection) {
                int count = 0;
                Collection<Object> collection = (Collection<Object>) value;
                for (Object object : collection) {
                    flattenedMap(result,
                            Collections.singletonMap("[" + (count++) + "]", object),
                            fullKey);
                }
                continue;
            }

            result.put(fullKey, value.toString());
        }
    }

}
