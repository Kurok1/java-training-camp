package indi.kurok1.curator.config.annotation;

import indi.kurok1.curator.config.ConfigType;
import indi.kurok1.curator.config.stream.DefaultJsonConfigAccumulator;
import indi.kurok1.curator.config.stream.StreamConfigAccumulator;

import java.lang.annotation.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 3.8.0
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ZookeeperConfigSources.class)
public @interface ZookeeperConfigSource {

    String path();

    ConfigType configType();

    boolean watchChange() default true;

    Class<? extends StreamConfigAccumulator> streamConfigAccumulator() default DefaultJsonConfigAccumulator.class;

}
