package com.acme.middleware.distrtibuted.cache;

import org.redisson.client.codec.StringCodec;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.stereotype.Component;

/**
 * inject codec
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Component
public class CodecRedissonAutoConfigurationCustomizer implements RedissonAutoConfigurationCustomizer {

    @Override
    public void customize(Config configuration) {
        //using string serialization
        configuration.setCodec(new StringCodec());
    }
}
