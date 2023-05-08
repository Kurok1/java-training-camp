package com.acme.middleware.rpc.client;

import com.acme.middleware.rpc.InvocationRequest;
import com.acme.middleware.rpc.service.ServiceInstance;

import java.lang.reflect.Method;

/**
 * 客户端请求拦截
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 * @see RpcClient
 */
public interface RequestInterceptor {

    boolean support(Method method, ServiceInstance serviceInstance, String serviceName, InvocationRequest request);

    default boolean beforeExecute(Method method, ServiceInstance serviceInstance, InvocationRequest request) throws Exception {
        return true;
    }

    default void afterExecute(Method method, ServiceInstance serviceInstance, InvocationRequest request, Object result, Exception ex) throws Exception {
    }

}
