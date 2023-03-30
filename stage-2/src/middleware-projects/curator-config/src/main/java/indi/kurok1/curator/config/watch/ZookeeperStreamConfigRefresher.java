package indi.kurok1.curator.config.watch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public final class ZookeeperStreamConfigRefresher {

    private ExecutorService executorService;

    public void initializeExecutors(int size) {
        this.executorService = Executors.newFixedThreadPool(size);
    }

    public void addRefreshTask(StreamRefreshTask task) {
        this.executorService.submit(task);
    }

}
