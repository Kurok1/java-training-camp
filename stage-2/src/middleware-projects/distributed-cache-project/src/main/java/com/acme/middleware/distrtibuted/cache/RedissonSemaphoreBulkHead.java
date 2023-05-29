package com.acme.middleware.distrtibuted.cache;

import org.redisson.RedissonSemaphore;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.TimeUnit;

/**
 * bulkhead implements by {@link org.redisson.api.RSemaphore}
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class RedissonSemaphoreBulkHead {

    private final String name;
    private final int limit;

    private final RedissonClient redissonClient;

    private final RSemaphore semaphore;


    public RedissonSemaphoreBulkHead(String name, int limit, RedissonClient redissonClient) {
        this.name = name;
        this.limit = limit;
        this.redissonClient = redissonClient;
        this.semaphore = redissonClient.getSemaphore(name);
        //清除所有可用许可
        this.semaphore.drainPermits();
        this.semaphore.release(limit);
    }

    public boolean tryAcquirePermission() {
        return this.semaphore.tryAcquire(1);

    }

    public void acquirePermission() {
        boolean permitted = tryAcquirePermission();
        if (permitted) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("current thread is interrupted");
        }

        throw new IllegalStateException("Bulkhead: " + name + " is full");
    }

    public void acquirePermission(int timeout, TimeUnit unit) {
        boolean permitted = tryAcquirePermission(timeout, unit);
        if (permitted) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("current thread is interrupted");
        }

        throw new IllegalStateException("Bulkhead: " + name + " is full");
    }

    public boolean tryAcquirePermission(int timeout, TimeUnit unit) {
        try {
            return semaphore.tryAcquire(1, timeout, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releasePermission() {
        this.semaphore.release(1);
    }
}
