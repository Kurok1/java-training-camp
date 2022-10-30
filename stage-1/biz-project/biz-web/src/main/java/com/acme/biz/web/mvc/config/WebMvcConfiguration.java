/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.biz.web.mvc.config;

import com.acme.biz.web.mvc.method.annotation.ApiResponseHandlerMethodReturnValueHandler;
import com.acme.biz.web.servlet.filter.GlobalCircuitBreakerFilter;
import com.acme.biz.web.servlet.filter.ResourceCircuitBreakerFilter;
import com.acme.biz.web.servlet.mvc.interceptor.ResourceBulkheadHandlerInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Web MVC 配置类
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since
 */
@Configuration
public class WebMvcConfiguration {

    @Autowired
    public void resetRequestMappingHandlerAdapter(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        List<HandlerMethodReturnValueHandler> oldReturnValueHandlers = requestMappingHandlerAdapter.getReturnValueHandlers();
        List<HandlerMethodReturnValueHandler> newReturnValueHandlers = new ArrayList<>(oldReturnValueHandlers);
        newReturnValueHandlers.add(0, new ApiResponseHandlerMethodReturnValueHandler());
        requestMappingHandlerAdapter.setReturnValueHandlers(newReturnValueHandlers);
    }

    @Bean
    public FilterRegistrationBean<GlobalCircuitBreakerFilter> globalCircuitBreakerFilter(CircuitBreakerRegistry circuitBreakerRegistry) {
        FilterRegistrationBean<GlobalCircuitBreakerFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new GlobalCircuitBreakerFilter(circuitBreakerRegistry));
        registrationBean.setName("globalCircuitBreakerFilter");
        registrationBean.setUrlPatterns(Collections.singleton("/*"));
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<ResourceCircuitBreakerFilter> resourceCircuitBreakerFilter(CircuitBreakerRegistry circuitBreakerRegistry) {
        FilterRegistrationBean<ResourceCircuitBreakerFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ResourceCircuitBreakerFilter(circuitBreakerRegistry));
        registrationBean.setName("resourceCircuitBreakerFilter");
        registrationBean.setUrlPatterns(Collections.singleton("/*"));
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

}
