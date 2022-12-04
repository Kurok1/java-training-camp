package com.acme.biz.web.configuration;

import com.acme.biz.web.interceptor.LoggingInterceptor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.context.annotation.Configuration;

/**
 * TODO
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Configuration
public class LoggingBeanPostProcessor extends AbstractAnnotationByteBuddyBeanPostProcessor<LoggingInterceptor.Log> {


    private final LoggingInterceptor loggingInterceptor = new LoggingInterceptor();

    @Override
    protected DynamicType.Unloaded<?> doIntercept(Class<?> beanType) {
        return new ByteBuddy()
                .subclass(beanType)
                .method(ElementMatchers.isAnnotatedWith(LoggingInterceptor.Log.class))
                .intercept(MethodDelegation.to(this.loggingInterceptor))
                .make();
    }

    @Override
    protected Class<LoggingInterceptor.Log> getAnnotationClass() {
        return LoggingInterceptor.Log.class;
    }
}
