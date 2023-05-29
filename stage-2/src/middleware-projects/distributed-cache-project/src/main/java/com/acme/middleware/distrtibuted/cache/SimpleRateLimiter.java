package com.acme.middleware.distrtibuted.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class SimpleRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private final String name;

    public final static int DEFAULT_RATE = 10;

    public final static int DEFAULT_REFRESH_INTERVAL = 1;

    public final static TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

    private int rate;

    public SimpleRateLimiter(StringRedisTemplate redisTemplate, String name) {
        this(redisTemplate, name, DEFAULT_RATE, DEFAULT_REFRESH_INTERVAL, DEFAULT_UNIT);
    }

    public SimpleRateLimiter(StringRedisTemplate redisTemplate, String name, int rate, int refreshInterval, TimeUnit unit) {
        this.redisTemplate = redisTemplate;
        Objects.requireNonNull(name);
        this.name = name;
        this.setRate(rate, refreshInterval, unit);
    }

    protected final String getPermitsKey() {
        return "{simple:permits}:" + this.name;
    }

    protected final String getRefreshConfigKey() {
        return "{simple:permits:config}:" + this.name;
    }

    public final void setRate(int rate, int refreshInterval, TimeUnit unit) {
        final String configKey = getRefreshConfigKey();
        final String valueKey = getPermitsKey();
        this.rate = rate;
        final String script = "redis.call('hset', KEYS[1], 'rate', ARGV[1]);"
                + "redis.call('hset', KEYS[1], 'interval', ARGV[2]);"
                + "redis.call('del', KEYS[2]);";
        this.redisTemplate.execute(RedisScript.of(script),
                Arrays.asList(configKey, valueKey),
                String.valueOf(rate), String.valueOf(unit.toMillis(refreshInterval))
        );
    }


    public final void acquire(int permits) {
        final String script = "local rate = redis.call('hget', KEYS[1], 'rate');"
                + "local interval = redis.call('hget', KEYS[1], 'interval');"
                + "assert(rate ~= false and interval ~= false, 'RateLimiter is not initialized')"

                + "assert(tonumber(rate) >= tonumber(ARGV[1]), 'Requested permits amount could not exceed defined rate'); "//客户端rate不一致场景

                + "local valueName = KEYS[2];"
                + "local currentValue = redis.call('get', valueName);" //获取当前时间窗口已经进入数量
                + "local result = -1;"                                  //结果定义, -1表示进入失败
                + "if currentValue ~= false then "

                    + "if tonumber(currentValue) + ARGV[2] <= tonumber(rate) then "
                        + "local nextValue = tonumber(currentValue) + ARGV[2]"
                        + "redis.call('INCRBY', KEYS[2], ARGV[2])"
                        + "result = tonumber(rate) - tonumber(currentValue) - ARGV[2]"
                    + "end; "
                + "else "
                    + "redis.call('set', valueName, ARGV[2], 'PX', interval); "
                    + "result = tonumber(rate) - ARGV[2]; "
                + "end; "
                + "return result;";
        RedisScript<Long> redisScript = RedisScript.of(script, Long.class);
        Long result = (Long) this.redisTemplate.execute(redisScript,
                Arrays.asList(getRefreshConfigKey(), getPermitsKey()),
                String.valueOf(rate), String.valueOf(permits));
        if (result == null || result < 0)
            throw new IllegalStateException("rate limited!");
    }
}
