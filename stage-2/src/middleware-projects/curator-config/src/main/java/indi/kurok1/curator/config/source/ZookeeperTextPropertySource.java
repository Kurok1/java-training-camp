package indi.kurok1.curator.config.source;

import org.springframework.core.env.PropertySource;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ZookeeperTextPropertySource extends PropertySource<String> {


    public ZookeeperTextPropertySource(String name, String source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        if (name.equals(super.name))
            return super.source;
        return null;
    }
}
