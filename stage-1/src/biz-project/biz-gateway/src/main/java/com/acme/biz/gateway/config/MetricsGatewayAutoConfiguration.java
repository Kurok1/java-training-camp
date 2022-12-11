package com.acme.biz.gateway.config;

import com.acme.biz.gateway.filter.factory.HttpRequestCounterGatewayFilterFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledFilter;
import org.springframework.context.annotation.Bean;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@ConditionalOnClass(MeterRegistry.class)
public class MetricsGatewayAutoConfiguration {

    @Bean
    @ConditionalOnEnabledFilter
    public HttpRequestCounterGatewayFilterFactory httpStatusCounterGatewayFilterFactory() {
        return new HttpRequestCounterGatewayFilterFactory();
    }

}
