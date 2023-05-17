package com.acme.config.server;

import com.acme.config.common.ConfigEntry;
import com.acme.config.common.ConfigKind;
import com.acme.config.common.ConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础配置初始化
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Component
public class ConfigServerInitializer implements ApplicationListener<ApplicationStartedEvent> {

    private final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ConfigManager configService = event.getApplicationContext().getBean(ConfigManager.class);

        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", Map.of("a", "b"));
        map.put("key3", List.of("1", "2", "3"));
        try {
            configService.saveConfig(prepareConfig("test-config-1", map));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigEntry prepareConfig(String configId, Map<String, Object> content) throws Exception {
        ConfigEntry configEntry = new ConfigEntry();
        configEntry.setConfigId(configId);
        String s = JSON.writeValueAsString(content);
        configEntry.setConfigType(ConfigKind.JSON.getName());
        configEntry.setContent(s);
        return configEntry;
    }
}
