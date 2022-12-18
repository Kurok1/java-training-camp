package com.acme.biz.api.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class SimpleEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private volatile boolean replaced = false;
    private final Object lock = new Object();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!replaced) {
            Map<String, Object> systemProperties = environment.getSystemProperties();
            synchronized (lock) {
                Map<String, Object> store = new ConcurrentHashMap<>(systemProperties);
                MutablePropertySources propertySources = environment.getPropertySources();
                //替换SystemProperties
                propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, new MapPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, store));
                replaced = true;
            }
        }
    }

}
