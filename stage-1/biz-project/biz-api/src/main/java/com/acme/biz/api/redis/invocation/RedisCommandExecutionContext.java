package com.acme.biz.api.redis.invocation;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * redis命令执行上下文
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class RedisCommandExecutionContext {

    private RedisConnection connection;
    private Method method;
    private Object[] parameters;
    private Object result;

    private final Map<String, Object> attributes = new HashMap<>();
    private long startNanos;//执行开始纳秒
    private long endNanos;  //执行结束纳秒
    private volatile boolean finished = false;

    private RedisSerializer<?> keySerializer;
    private RedisSerializer<?> valueSerializer;
    private volatile Exception exception;

    public RedisConnection getConnection() {
        return connection;
    }

    public void setConnection(RedisConnection connection) {
        this.connection = connection;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(this.attributes);
    }

    public void setAttribute(String key, Object value) {
        this.attributes.putIfAbsent(key, value);
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void start() {
        this.startNanos = System.nanoTime();
    }

    public void finish() {
        this.endNanos = System.nanoTime();
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public RedisSerializer<?> getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(RedisSerializer<?> keySerializer) {
        this.keySerializer = keySerializer;
    }

    public RedisSerializer<?> getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(RedisSerializer<?> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Object execute() throws Exception {
        Object result = this.method.invoke(connection, parameters);
        this.result = result;
        return result;
    }

    public static class Builder {
        private final RedisCommandExecutionContext context = new RedisCommandExecutionContext();

        public Builder(RedisConnection redisConnection, Method method) {
            this.context.setConnection(redisConnection);
            this.context.setMethod(method);
        }

        public Builder parameters(Object[] parameters) {
            this.context.setParameters(parameters);
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.context.setAttribute(key, value);
            return this;
        }

        public Builder keySerializer(RedisSerializer<?> keySerializer) {
            this.context.setKeySerializer(keySerializer);
            return this;
        }

        public Builder valueSerializer(RedisSerializer<?> valueSerializer) {
            this.context.setValueSerializer(valueSerializer);
            return this;
        }

        public RedisCommandExecutionContext build() {
            return this.context;
        }
    }
}
