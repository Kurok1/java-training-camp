package indi.kurok1.curator.config.annotation;

import indi.kurok1.curator.config.ZookeeperConfigBeanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ZookeeperConfigBeanRegistrar.class)
public @interface EnableZookeeperConfig {

    String globalPath() default "";

    String connectString() default "localhost:2181";

    String namespace() default "";

    int sessionTimeoutMs() default 30000;

    int connectionTimeoutMs() default 30000;
}
