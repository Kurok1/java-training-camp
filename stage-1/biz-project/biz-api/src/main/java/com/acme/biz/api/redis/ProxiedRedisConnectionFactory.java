package com.acme.biz.api.redis;

import com.acme.biz.api.redis.invocation.RedisCommandExecutor;
import com.acme.biz.api.redis.invocation.RedisCommandInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * TODO
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ProxiedRedisConnectionFactory implements RedisConnectionFactory {

    private final RedisConnectionFactory delegate;
    protected final ObjectProvider<RedisCommandInterceptor> interceptors;
    private final RedisSerializer<?> keySerializer;
    private final RedisSerializer<?> valueSerializer;

    public ProxiedRedisConnectionFactory(RedisConnectionFactory delegate, ObjectProvider<RedisCommandInterceptor> interceptors, RedisSerializer<?> keySerializer, RedisSerializer<?> valueSerializer) {
        Assert.notNull(delegate, "redis connection factory cannot be null");
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.interceptors = interceptors;
        this.delegate = delegate;
    }

    @Override
    public RedisConnection getConnection() {
        RedisConnection connection = delegate.getConnection();
        return newRedisConnectionProxy(connection);
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return delegate.getClusterConnection();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return delegate.getConvertPipelineAndTxResults();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return delegate.getSentinelConnection();
    }

    @Override
    @Nullable
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return delegate.translateExceptionIfPossible(ex);
    }

    protected RedisConnection newRedisConnectionProxy(RedisConnection realRedisConnection) {
        ClassLoader classLoader = realRedisConnection.getClass().getClassLoader();
        InvocationHandler invocationHandler = new RedisCommandExecutor(realRedisConnection, this.interceptors, keySerializer, valueSerializer);
        return (RedisConnection) Proxy.newProxyInstance(classLoader, new Class[]{RedisConnection.class}, invocationHandler);
    }
}
