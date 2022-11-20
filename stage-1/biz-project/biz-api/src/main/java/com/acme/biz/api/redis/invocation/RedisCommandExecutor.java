package com.acme.biz.api.redis.invocation;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.OrderComparator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis Command动态代理
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class RedisCommandExecutor implements InvocationHandler {

    private final RedisConnection delegate;
    private final ObjectProvider<RedisCommandInterceptor> redisCommandInterceptors;

    private final RedisSerializer<?> keySerializer;
    private final RedisSerializer<?> valueSerializer;

    private List<RedisCommandInterceptor> interceptors;

    public RedisCommandExecutor(RedisConnection delegate, ObjectProvider<RedisCommandInterceptor> redisCommandInterceptors, RedisSerializer<?> keySerializer, RedisSerializer<?> valueSerializer) {
        this.delegate = delegate;
        this.redisCommandInterceptors = redisCommandInterceptors;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //todo 执行
        //构建执行上下文
        RedisCommandExecutionContext context = buildExecutionContext(method, args);
        //构建执行链路
        RedisCommandExecutionChain executionChain = new DefaultRedisCommandExecutionChain(getInterceptors());
        //执行
        return executionChain.execute(context);
    }

    public List<RedisCommandInterceptor> getInterceptors() {
        if (interceptors == null) {
            //初始化
            final List<RedisCommandInterceptor> interceptors = new ArrayList<>();
            this.redisCommandInterceptors.stream().forEach(interceptors::add);
            if (interceptors.isEmpty()) {
                this.interceptors = new ArrayList<>();
            } else {
                //排序
                this.interceptors = interceptors.stream()
                        .sorted(OrderComparator.INSTANCE)
                        .collect(Collectors.toList());
            }
        }
        return this.interceptors;
    }

    protected RedisCommandExecutionContext buildExecutionContext(Method method, Object[] args) {
        RedisCommandExecutionContext.Builder builder = new RedisCommandExecutionContext.Builder(this.delegate, method);


        return builder.parameters(args)
                .keySerializer(this.keySerializer)
                .valueSerializer(this.valueSerializer)
                .build();
    }

}
