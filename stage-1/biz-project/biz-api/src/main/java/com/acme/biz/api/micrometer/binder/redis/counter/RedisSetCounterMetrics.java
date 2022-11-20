package com.acme.biz.api.micrometer.binder.redis.counter;

import com.acme.biz.api.micrometer.binder.redis.AbstractRedisMetrics;
import com.acme.biz.api.micrometer.binder.redis.gauge.RedisSetGaugeMetrics;
import com.acme.biz.api.redis.invocation.RedisCommandExecutionContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.data.redis.connection.RedisStringCommands;

import java.lang.reflect.Method;

/**
 * 统计set方法执行次数
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Import(RedisSetGaugeMetrics.class)
public class RedisSetCounterMetrics extends AbstractRedisMetrics<Counter> {

    /**
     * counter名称模板，精确到key
     */
    public static final String GENERIC_COUNTER_NAME = "COUNT.redis.value.keys.set";

    public static final String GLOBAL_COUNTER_NAME = "COUNT.redis.value.keys.total-set";

    @Override
    protected boolean determineMonitoring(RedisCommandExecutionContext context) {
        Method method = context.getMethod();
        Class<?> declaringClass = method.getDeclaringClass();
        String methodName = method.getName();
        // org.springframework.data.redis.connection.RedisStringCommands.set(byte[], byte[])
        return RedisStringCommands.class.equals(declaringClass) && "set".equals(methodName) && method.getParameterCount() == 2;
    }

    protected Counter buildGlobalMeter(RedisCommandExecutionContext executionContext, MeterRegistry meterRegistry) {
        boolean success = (executionContext.getException() == null && Boolean.TRUE.equals(executionContext.getResult()));
        String successTag = success ? "true" : "false";
        return Counter.builder(GLOBAL_COUNTER_NAME)
                .tag("succeed", successTag)
                .register(meterRegistry);
    }

    @Override
    protected Counter buildMeter(RedisCommandExecutionContext executionContext, MeterRegistry meterRegistry) {
        Object key = getKey(executionContext, 0);
        String successTag = executionContext.getException() == null ? "true" : "false";
        return Counter.builder(String.format(GENERIC_COUNTER_NAME))
                .tag("key", key.toString())
                .tag("succeed", successTag)
                .register(meterRegistry);
    }

    @Override
    protected void doRecordAfterExecute(Counter meter, RedisCommandExecutionContext executionContext) {
        //当前key+1
        meter.increment();
        //全局key+1
        buildGlobalMeter(executionContext, getMeterRegistry()).increment();

        final Object key = getKey(executionContext, 0);
        //为当前key，注册Gauge
        Gauge.builder(RedisSetGaugeMetrics.GENERIC_GAUGE_NAME, getMeterRegistry(), (registry) -> RedisSetGaugeMetrics.getGenericSuccessRate(registry, key.toString()))
                .tag("key", key.toString())
                .register(getMeterRegistry());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        super.bindTo(registry);
    }

    @Override
    public int getOrder() {
        //最高优先级
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
