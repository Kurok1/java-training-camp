package com.acme.middleware.distrtibuted.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@SpringBootTest(classes = Bootstrap.class)
public class RedissonRateLimiterTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private SimpleRateLimiter rateLimiter;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    @BeforeEach
    public void setRateLimiter() {
        rateLimiter = new SimpleRateLimiter(redisTemplate, "test-rate");
        rateLimiter.setRate(2, 1, TimeUnit.SECONDS);
    }

    @Test
    public void testRateLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            executorService.execute(this::task);
        }
        Thread.sleep(1000);
        for (int i = 0; i < 3; i++) {
            executorService.execute(this::task);
        }

        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    public void task() {
        this.rateLimiter.acquire(1);
        System.out.print("current:  "  + System.currentTimeMillis());
    }

}
