package indi.kurok1.curator.config.watch;

import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ZookeeperConfigChangedEvent extends ApplicationEvent {

    private final String path;

    private final Collection<String> diffKeys;

    public ZookeeperConfigChangedEvent(String path, Collection<String> diffKeys) {
        super(path);
        this.path = path;
        this.diffKeys = diffKeys;
    }

    public String getPath() {
        return path;
    }

    public Collection<String> getDiffKeys() {
        return diffKeys;
    }
}
