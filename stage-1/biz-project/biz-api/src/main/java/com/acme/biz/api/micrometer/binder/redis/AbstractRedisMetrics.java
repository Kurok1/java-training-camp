package com.acme.biz.api.micrometer.binder.redis;

import com.acme.biz.api.redis.invocation.RedisCommandExecutionChain;
import com.acme.biz.api.redis.invocation.RedisCommandExecutionContext;
import com.acme.biz.api.redis.invocation.RedisCommandInterceptor;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.Map;

/**
 * 抽象实现, 依赖{@link RedisCommandInterceptor}
 * <pre>
 * invoke->
 *     counter->
 *          timer->
 *              redis command
 *          timer<-
 *     counter<-
 * return
 * </pre>
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 * @see com.acme.biz.api.redis.invocation.RedisCommandInterceptor
 */
public abstract class AbstractRedisMetrics<M extends Meter> implements RedisCommandInterceptor, MeterBinder {

    private MeterRegistry meterRegistry;

    @Override
    public Object execute(RedisCommandExecutionContext executionContext, RedisCommandExecutionChain executionChain) {
        //判断是否拦截
        if (!determineMonitoring(executionContext)) {
            return executionChain.execute(executionContext);
        }

        doRecordBeforeExecute(buildMeter(executionContext, this.meterRegistry), executionContext);
        //链路继续传递下去
        Object result = executionChain.execute(executionContext);

        doRecordAfterExecute(buildMeter(executionContext, this.meterRegistry), executionContext);
        return result;
    }


    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;
    }

    /**
     * @param context 执行上下文
     * @return 判断当前操作是否应该被监控
     */
    protected abstract boolean determineMonitoring(RedisCommandExecutionContext context);

    /**
     * 根据执行上下文构建{@link Meter},并注册到{@link MeterRegistry}
     * @param executionContext redis执行上下文
     * @param meterRegistry 注册上下文
     * @return {@link Meter} 具体实例
     */
    protected abstract M buildMeter(RedisCommandExecutionContext executionContext, MeterRegistry meterRegistry);

    /**
     * 获取redis操作的key,从参数列表中，指定位置参数，反序列化得到
     * @param executionContext 执行上下文
     * @param keyParameterIndex  key参数位置
     * @return redis操作key
     */
    protected Object getKey(RedisCommandExecutionContext executionContext, int keyParameterIndex) {
        final String keyAttribute = "redis.execute.key";
        Map<String, Object> attributes = executionContext.getAttributes();
        if (attributes.containsKey(keyAttribute)) {
            return attributes.get(keyAttribute);
        }


        Object[] parameter = executionContext.getParameters();
        if (parameter == null || parameter.length == 0)
            return null;// no key

        if (keyParameterIndex < 0 || keyParameterIndex >= parameter.length)
            throw new IllegalArgumentException("illegal key parameter index : " + keyParameterIndex);

        byte[] bytes = (byte[]) parameter[keyParameterIndex];
        Object key = executionContext.getKeySerializer().deserialize(bytes);
        //写入上下文
        executionContext.setAttribute(keyAttribute, key);
        return key;
    }

    /**
     * 执行前的记录操作，通常可能是数据初始化
     * @param meter meter
     * @param executionContext 执行上下文
     */
    protected void doRecordBeforeExecute(M meter, RedisCommandExecutionContext executionContext) {

    }

    /**
     * 执行后的记录操作，一般是记录结果，应该考虑异步操作
     * @param meter meter
     * @param executionContext 执行上下文
     */
    protected void doRecordAfterExecute(M meter, RedisCommandExecutionContext executionContext) {

    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
