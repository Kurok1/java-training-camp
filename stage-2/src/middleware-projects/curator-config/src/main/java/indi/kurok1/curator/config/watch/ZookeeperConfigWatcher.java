package indi.kurok1.curator.config.watch;

import indi.kurok1.curator.config.source.ZookeeperConfigUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Collection;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ZookeeperConfigWatcher implements CuratorWatcher,
        ApplicationEventPublisherAware, EnvironmentAware {

    public static final String WATCHER_BEAN_NAME = "zookeeperConfigWatcher";

    private ApplicationEventPublisher publisher;
    private ConfigurableEnvironment environment;

    private CuratorFramework client;

    @Override
    public void process(WatchedEvent event) throws Exception {
        Watcher.Event.EventType eventType = event.getType();
        if (eventType == Watcher.Event.EventType.NodeDataChanged) {
            String path = event.getPath();
            //修改对应目标配置
            Collection<String> diffKeys = ZookeeperConfigUtils.retrieveConfigAndSave(environment, client, path);
            ZookeeperConfigChangedEvent changedEvent = new ZookeeperConfigChangedEvent(path, diffKeys);
            this.publisher.publishEvent(changedEvent);
        }
    }


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    public void setClient(CuratorFramework client) {
        this.client = client;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }
}
