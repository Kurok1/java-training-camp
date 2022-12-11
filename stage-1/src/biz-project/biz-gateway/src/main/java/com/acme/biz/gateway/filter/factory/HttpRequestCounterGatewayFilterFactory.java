package com.acme.biz.gateway.filter.factory;

import com.acme.biz.gateway.filter.HttpRequestCounterGatewayFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class HttpRequestCounterGatewayFilterFactory extends AbstractGatewayFilterFactory<HttpRequestCounterGatewayFilter.HttpRequestCounterConfig>
                            implements MeterBinder {

    private MeterRegistry meterRegistry;

    public HttpRequestCounterGatewayFilterFactory() {
        super(HttpRequestCounterGatewayFilter.HttpRequestCounterConfig.class);
    }

    @Override
    public GatewayFilter apply(HttpRequestCounterGatewayFilter.HttpRequestCounterConfig config) {
        return new HttpRequestCounterGatewayFilter(config, this.meterRegistry);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;
    }
}
