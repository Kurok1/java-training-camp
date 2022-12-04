package com.acme.biz.web.interceptor;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * TODO
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class LoggingInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @RuntimeType
    public Object doLog(@Origin Method method, @SuperCall Callable<?> callable) {
        // intercept any method of any signature
        logger.info("before invoke ...");
        long start = System.currentTimeMillis();
        try {
            return callable.call();
        } catch (Exception e) {
            logger.error("error occupied", e);
            throw new RuntimeException(e);
        } finally {
            System.out.println(method + "invoke finished, it took " + (System.currentTimeMillis() - start));
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Log {

    }

}
