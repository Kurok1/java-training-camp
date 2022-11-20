package com.acme.biz.api.redis;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启Redis操作拦截
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 * @see RedisCommandInterceptorSelector
 * @see RedisTemplateBeanPostProcessor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        RedisCommandInterceptorSelector.class,
        RedisTemplateBeanPostProcessor.class
})
public @interface EnableRedisIntercepting {
}
