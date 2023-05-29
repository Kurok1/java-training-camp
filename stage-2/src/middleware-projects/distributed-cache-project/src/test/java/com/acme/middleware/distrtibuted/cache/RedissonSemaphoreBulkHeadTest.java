package com.acme.middleware.distrtibuted.cache;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = Bootstrap.class)
class RedissonSemaphoreBulkHeadTest {

    @Autowired
    private RedissonClient redissonClient;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);


    @Test
    public void testBulkhead() throws InterruptedException {
        RedissonSemaphoreBulkHead bulkHead = new RedissonSemaphoreBulkHead("test", 5, this.redissonClient);
        for (int i = 0; i < 10; i++)
            executorService.execute(task(bulkHead));

        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    Runnable task(final RedissonSemaphoreBulkHead bulkHead) {
        return () -> {
            bulkHead.acquirePermission(2, TimeUnit.SECONDS);

            System.out.println("do somethings on " + System.currentTimeMillis());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            bulkHead.releasePermission();
        };
    }

}