package com.acme.config.common.util.resolve;

import com.acme.config.common.ConfigKind;
import com.acme.config.common.util.FlattenedMapUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class JsonConfigResolver implements ConfigResolver {

    private final ObjectMapper JSON = new ObjectMapper();

    public static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() { };

    @Override
    public ConfigKind supportKind() {
        return ConfigKind.JSON;
    }

    @Override
    public Map<String, String> apply(String s) {
        Map<String, Object> origin = null;
        try {
            origin = this.JSON.readValue(s, MAP_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> result = new HashMap<>();
        FlattenedMapUtils.flattenedMap(result, origin, "");
        return result;
    }
}
