package com.acme.middleware.rpc.server;

import com.acme.middleware.rpc.InvocationRequest;
import com.acme.middleware.rpc.InvocationResponse;

/**
 * rpc执行拦截
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public interface RpcInvokeInterceptor {

    default void beforeInvoke(Object service, InvocationRequest request) {

    }


    default void afterInvoke(Object service, InvocationRequest request, InvocationResponse response) {

    }

}
