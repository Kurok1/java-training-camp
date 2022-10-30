package com.acme.biz.web.servlet.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.core.registry.AbstractRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Component
public class CircuitBreakerRefresher implements ApplicationListener<EnvironmentChangeEvent>,
        ApplicationContextAware, EnvironmentAware, InitializingBean {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreakerProperties circuitBreakerProperties;
    private ApplicationContext applicationContext;
    private Environment environment;
    private Binder binder;
    //监听配置列表
    private static final String CIRCUIT_BREAKER_CONFIG_PREFIX = "resilience4j.circuitbreaker.backends.";
    //归档的circuit-breaker绑定的名称
    private static final String ARCHIVED_CIRCUIT_BREAKER_SUFFIX = "@OLD-";

    /**
     * 配置转换成CircuitBreakerConfig
     */
    public static Function<CircuitBreakerConfigurationProperties.InstanceProperties, CircuitBreakerConfig> circuitBreakerConfigFunction = properties -> {
        if (properties == null)
            return CircuitBreakerConfig.ofDefaults();
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();
        //inject config
        if (properties.getWaitDurationInOpenState() != null)
            builder.waitDurationInOpenState(properties.getWaitDurationInOpenState());
        if (properties.getSlowCallDurationThreshold() != null)
            builder.slowCallDurationThreshold(properties.getSlowCallDurationThreshold());
        if (properties.getMaxWaitDurationInHalfOpenState() != null)
            builder.maxWaitDurationInHalfOpenState(properties.getMaxWaitDurationInHalfOpenState());
        if (properties.getFailureRateThreshold() != null)
            builder.failureRateThreshold(properties.getFailureRateThreshold());
        if (properties.getSlowCallDurationThreshold() != null)
            builder.slowCallDurationThreshold(properties.getSlowCallDurationThreshold());
        if (properties.getSlidingWindowType() != null)
            builder.slidingWindowType(properties.getSlidingWindowType());
        if (properties.getSlidingWindowSize() != null)
            builder.slidingWindowSize(properties.getSlidingWindowSize());
        if (properties.getMinimumNumberOfCalls() != null)
            builder.minimumNumberOfCalls(properties.getMinimumNumberOfCalls());
        if (properties.getPermittedNumberOfCallsInHalfOpenState() != null)
            builder.permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState());
        if (properties.getAutomaticTransitionFromOpenToHalfOpenEnabled() != null)
            builder.automaticTransitionFromOpenToHalfOpenEnabled(properties.getAutomaticTransitionFromOpenToHalfOpenEnabled());
        if (properties.getWritableStackTraceEnabled() != null)
            builder.writableStackTraceEnabled(properties.getWritableStackTraceEnabled());
        if (properties.getRecordFailurePredicate() != null) {
            try {
                builder.recordException(properties.getRecordFailurePredicate().getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        if (properties.getRecordExceptions() != null) {
            builder.recordExceptions(properties.getRecordExceptions());
        }
        if (properties.getIgnoreExceptions() != null)
            builder.ignoreExceptions(properties.getIgnoreExceptions());

        return builder.build();
    };

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        final Set<String> keys = event.getKeys();
        if (this.applicationContext.equals(event.getSource())
                // Backwards compatible
                || keys.equals(event.getSource())) {
            synchronized (this.circuitBreakerRegistry) {
                doRefreshCircuitBreaker(keys);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //initialize binder
        this.binder = new Binder(ConfigurationPropertySources.get(this.environment));
    }

    protected void doRefreshCircuitBreaker(Set<String> changedKeys) {
        rebindCircuitBreakerProperties();

        final Set<String> effectCircuitBreakerNames = changedKeys.stream()
                .filter(filterConfigKey)
                .map(getCircuitBreakerNameFunction)
                .collect(Collectors.toSet());
        if (effectCircuitBreakerNames.isEmpty())
            return;

        effectCircuitBreakerNames.forEach(this::refreshCircuitBreak);
    }

    /**
     * 读取配置源，绑定至CircuitBreakerProperties
     */
    private void rebindCircuitBreakerProperties() {
        BindResult<CircuitBreakerProperties> result = binder.bind("resilience4j.circuitbreaker", CircuitBreakerProperties.class);
        this.circuitBreakerProperties = result.get();
    }

    /**
     * 刷新指定名称的circuit-breaker
     * @param name name of circuit-breaker
     */
    protected void refreshCircuitBreak(String name) {
        //1.刷新CircuitBreaker配置
        CircuitBreakerConfig config = this.circuitBreakerProperties.findCircuitBreakerProperties(name)
                .map(circuitBreakerConfigFunction)
                .orElse(CircuitBreakerConfig.ofDefaults());
        //2.找到已经存在的CircuitBreaker
        CircuitBreaker existedCircuitBreaker = findExistedCircuitBreaker(name);
        if (existedCircuitBreaker == null) {
            //没有创建过，直接创建CircuitBreaker
            this.circuitBreakerRegistry.circuitBreaker(name, config);
        } else {
            //替换实现老版本
            CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);
            this.circuitBreakerRegistry.replace(name, circuitBreaker);
            //老版本归档处理
            String archivedName = name + ARCHIVED_CIRCUIT_BREAKER_SUFFIX + System.currentTimeMillis();
            reflectiveArchiveCircuitBreaker(new ArchivedCircuitBreaker(archivedName, existedCircuitBreaker), this.circuitBreakerRegistry);
        }

    }

    /**
     * 反射调用，将旧版的circuit-breaker归档处理
     * @param archivedCircuitBreaker 归档后的circuit-break
     * @param registry circuit-breaker注册中心
     */
    protected void reflectiveArchiveCircuitBreaker(final ArchivedCircuitBreaker archivedCircuitBreaker, CircuitBreakerRegistry registry) {
        if (registry instanceof AbstractRegistry) {
            AbstractRegistry<CircuitBreaker, CircuitBreakerConfig> abstractRegistry = (AbstractRegistry<CircuitBreaker, CircuitBreakerConfig>) registry;
            try {
                Method method = AbstractRegistry.class.getDeclaredMethod("computeIfAbsent", String.class, Supplier.class);
                method.setAccessible(true);
                method.invoke(abstractRegistry, archivedCircuitBreaker.getName(), new Supplier<CircuitBreaker>() {
                    @Override
                    public CircuitBreaker get() {
                        return archivedCircuitBreaker;
                    }
                });
                method.setAccessible(false);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

        }
    }

    protected CircuitBreaker findExistedCircuitBreaker(String name) {
        if (ObjectUtils.isEmpty(name))
            throw new NullPointerException();
        try {
            return this.circuitBreakerRegistry.getAllCircuitBreakers().find(circuitBreaker -> name.equals(circuitBreaker.getName())).get();
        } catch (NoSuchElementException ignored) {
            //no circuit-breaker, so return null
            return null;
        }

    }

    private static final Predicate<String> filterConfigKey = configKey -> configKey.startsWith(CIRCUIT_BREAKER_CONFIG_PREFIX);

    /**
     * like "resilience4j.circuitbreaker.configs.name.xxx" -> "name"
     */
    private static final Function<String, String> getCircuitBreakerNameFunction = configKey -> {
        String replacePrefix = configKey.substring(CIRCUIT_BREAKER_CONFIG_PREFIX.length());
        int firstDotIndex = replacePrefix.indexOf('.');
        if (firstDotIndex == -1)
            return "";
        return replacePrefix.substring(0, firstDotIndex);
    };

}
