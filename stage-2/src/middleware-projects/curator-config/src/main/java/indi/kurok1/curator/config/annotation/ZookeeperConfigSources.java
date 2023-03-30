package indi.kurok1.curator.config.annotation;

import java.lang.annotation.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 3.8.0
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZookeeperConfigSources {

    ZookeeperConfigSource[] value();

}
