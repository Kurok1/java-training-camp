package indi.kurok1.curator.config.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class MapConfigUtils {

    public static final String DOT = ".";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final XmlMapper XML = new XmlMapper();

    public static Map<String, Object> resolveByJson(String jsonValue) throws IOException {
        LinkedHashMap<String, Object> dataMap = JSON.readValue(jsonValue,
                LinkedHashMap.class);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(64);
        flattenedMap(result, dataMap, "");
        return result;
    }

    public static Map<String, Object> resolveByJson(byte[] jsonValue) throws IOException {
        LinkedHashMap<String, Object> dataMap = JSON.readValue(jsonValue,
                LinkedHashMap.class);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(64);
        flattenedMap(result, dataMap, "");
        return result;
    }

    public static Map<String, Object> resolveByXml(String xmlValue) throws IOException {
        LinkedHashMap<String, Object> dataMap = XML.readValue(xmlValue,
                LinkedHashMap.class);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(64);
        flattenedMap(result, dataMap, "");
        return result;
    }

    public static Map<String, Object> resolveByXml(byte[] jsonValue) throws IOException {
        LinkedHashMap<String, Object> dataMap = XML.readValue(jsonValue,
                LinkedHashMap.class);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(64);
        flattenedMap(result, dataMap, "");
        return result;
    }


    /**
     * copy by com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config:AbstractPropertySourceLoader
     * @param result
     * @param dataMap
     * @param parentKey
     */
    protected static void flattenedMap(Map<String, Object> result, Map<String, Object> dataMap,
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

            String fullKey = ObjectUtils.isEmpty(parentKey) ? key : key.startsWith("[")
                    ? parentKey.concat(key) : parentKey.concat(DOT).concat(key);

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

            result.put(fullKey, value);
        }
    }

}
