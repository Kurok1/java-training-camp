package com.acme.biz.web.configuration;

import net.bytebuddy.dynamic.DynamicType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 基于{@link net.bytebuddy.ByteBuddy}实现的注解拦截
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public abstract class AbstractAnnotationByteBuddyBeanPostProcessor<A extends Annotation> implements InstantiationAwareBeanPostProcessor,
        BeanClassLoaderAware, BeanFactoryAware {

    private ClassLoader classLoader;
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.autowireCapableBeanFactory = (AutowireCapableBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanType, String beanName) throws BeansException {
        if (hasLogAnnotatedMethod(beanType, getAnnotationClass())) {
            Class<?> dynamicType = doIntercept(beanType).load(this.classLoader)
                    .getLoaded();
            try {
                return this.autowireCapableBeanFactory.createBean(dynamicType);
            } catch (Exception e) {
                throw new BeanCreationException(e.getMessage());
            }
        }

        return null;
    }

    protected abstract DynamicType.Unloaded<?> doIntercept(Class<?> beanType);

    protected abstract Class<A> getAnnotationClass();

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    protected AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
        return autowireCapableBeanFactory;
    }

    /**
     * 是否存在被指定注解标记的方法
     * @param beanClass 被代理的bean class
     * @param annotationClass 注解
     * @return 存在与否
     */
    public boolean hasLogAnnotatedMethod(Class<?> beanClass, Class<A> annotationClass) {
        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(annotationClass))
                return true;
        }
        return false;
    }
}
