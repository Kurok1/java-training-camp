package com.acme.config.sample;

import com.acme.config.client.ClientConfiguration;
import com.acme.config.client.ConfigManagerProvider;
import com.acme.config.common.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class App {

    public static void main(String[] args) {
        ClientConfiguration configuration = new ClientConfiguration.Builder()
                .interest("test-config-1", ConfigKind.JSON)
                .build(UUID.randomUUID().toString(), "http://localhost:8080");
        ConfigManager configManager = ConfigManagerProvider.getConfigManager(configuration);
        ConfigService configService = configManager.getConfigService();


        configService.registerWatcher("test-config-1", (configId, changedConfigs) -> {
            List<String> keys = changedConfigs.stream().map(ConfigWatcher.ChangedConfigEntry::getKey).collect(Collectors.toList());
            String template = "配置[%s]发生变化, 变化keys: [%s]\n";
            System.out.printf(template, configId, keys);
        });


        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String value = configService.getProperty("key1");
            System.out.println("key1: " + value);
        }

    }

}
