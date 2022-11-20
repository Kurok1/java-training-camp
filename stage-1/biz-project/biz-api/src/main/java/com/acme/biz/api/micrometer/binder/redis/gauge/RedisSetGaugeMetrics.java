package com.acme.biz.api.micrometer.binder.redis.gauge;

import com.acme.biz.api.micrometer.binder.redis.counter.RedisSetCounterMetrics;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * 统计set方法执行成功率
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class RedisSetGaugeMetrics implements MeterBinder {

    public static final String GENERIC_GAUGE_NAME = "GAUGE.redis.value.keys.set";
    public static final String GLOBAL_GAUGE_NAME = "GAUGE.redis.value.keys.total-set";

    @Override
    public void bindTo(MeterRegistry registry) {
        //全局key set的成功调用比率注册
        Gauge.builder(GLOBAL_GAUGE_NAME, registry, RedisSetGaugeMetrics::getGlobalSuccessRate).register(registry);

    }

    /**
     * 获取所有key的成功调用比率
     * @param meterRegistry 注册中心
     * @return 成功调用比率
     */
    public static double getGlobalSuccessRate(MeterRegistry meterRegistry) {
        //获取全局Counter
        final String GLOBAL_COUNT_NAME = RedisSetCounterMetrics.GLOBAL_COUNTER_NAME;
        //全局调用次数
        Counter total = meterRegistry.find(GLOBAL_COUNT_NAME).counter();
        double totalCount = total.count();
        //成功次数
        Counter succeed = meterRegistry.find(GLOBAL_COUNT_NAME).tags(Tags.of(Tag.of("succeed", "true"))).counter();
        double succeedCount = succeed.count();

        return succeedCount / totalCount;
    }

    public static double getGenericSuccessRate(MeterRegistry registry, String key) {
        //获取Counter
        final String GENERIC_COUNT_NAME = RedisSetCounterMetrics.GENERIC_COUNTER_NAME;
        //全局调用次数
        Counter total = registry.find(GENERIC_COUNT_NAME).tags(Tags.of(Tag.of("key", key))).counter();
        double totalCount = total.count();
        //成功次数
        Counter succeed = registry.find(GENERIC_COUNT_NAME).tags(Tags.of(Tag.of("key", key), Tag.of("succeed", "true"))).counter();
        double succeedCount = succeed.count();

        return succeedCount / totalCount;
    }


}
