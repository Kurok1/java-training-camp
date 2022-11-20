package com.acme.biz.api.redis;

import com.acme.biz.api.redis.invocation.RedisCommandInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 替换{@link RedisConnectionFactory} 实现
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
public class RedisTemplateBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private ObjectProvider<RedisCommandInterceptor> redisCommandInterceptors;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        if (RedisTemplate.class.isAssignableFrom(beanClass)) {
            RedisTemplate redisTemplate = (RedisTemplate) bean;
            RedisConnectionFactory redisConnectionFactory = redisTemplate.getConnectionFactory();
            RedisSerializer<?> keySerializer = redisTemplate.getKeySerializer();
            RedisSerializer<?> valueSerializer = redisTemplate.getValueSerializer();
            //替换Connection Factory
            ((RedisTemplate) bean).setConnectionFactory(new ProxiedRedisConnectionFactory(redisConnectionFactory, redisCommandInterceptors, keySerializer, valueSerializer));
        }
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        //延迟注入
        this.redisCommandInterceptors = beanFactory.getBeanProvider(RedisCommandInterceptor.class);
    }
}
