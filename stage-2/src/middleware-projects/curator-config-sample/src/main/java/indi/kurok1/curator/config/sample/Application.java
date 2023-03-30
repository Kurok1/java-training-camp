package indi.kurok1.curator.config.sample;

import indi.kurok1.curator.config.ConfigType;
import indi.kurok1.curator.config.annotation.EnableZookeeperConfig;
import indi.kurok1.curator.config.annotation.ZookeeperConfigSource;
import indi.kurok1.curator.config.watch.ZookeeperConfigChangedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collection;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Configuration
@EnableZookeeperConfig
@ZookeeperConfigSource(path = "/config-path/test", configType = ConfigType.TEXT)
@ZookeeperConfigSource(path = "/config-path/test.json", configType = ConfigType.JSON)
@ZookeeperConfigSource(path = "/config-path/config-stream", configType = ConfigType.EVENT_STREAM)
public class Application {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Application.class);
        context.addApplicationListener(myListener());
        Environment environment = context.getEnvironment();

        System.out.println(environment.getProperty("test"));
        System.out.println(environment.getProperty("key2[0].a1"));
        while (true) {

        }
    }

    private static ApplicationListener<ZookeeperConfigChangedEvent> myListener() {
        return event -> {
            final String path = event.getPath();
            Collection<String> keys = event.getDiffKeys();
            System.out.println("path : " + path + "has changed, diffKeys is " + keys.toString());
        };
    }

}
