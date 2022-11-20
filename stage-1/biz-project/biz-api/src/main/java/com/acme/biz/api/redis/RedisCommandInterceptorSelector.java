package com.acme.biz.api.redis;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * TODO
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class RedisCommandInterceptorSelector implements ImportSelector, BeanClassLoaderAware {

    private ClassLoader classLoader;

    protected Class<?> getSpringFactoriesLoaderFactoryClass() {
        return EnableRedisIntercepting.class;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        List<String> beanNames = SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(), classLoader);
        return StringUtils.toStringArray(beanNames);
    }



    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
