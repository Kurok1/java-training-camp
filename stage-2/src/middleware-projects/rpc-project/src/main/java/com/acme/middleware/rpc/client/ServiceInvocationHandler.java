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
package com.acme.middleware.rpc.client;

import com.acme.middleware.rpc.InvocationRequest;
import com.acme.middleware.rpc.loadbalancer.ServiceInstanceSelector;
import com.acme.middleware.rpc.service.ServiceInstance;
import com.acme.middleware.rpc.service.registry.ServiceRegistry;
import io.netty.channel.ChannelFuture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.acme.middleware.rpc.client.ExchangeFuture.createExchangeFuture;
import static com.acme.middleware.rpc.client.ExchangeFuture.removeExchangeFuture;

/**
 * 服务调用处理
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class ServiceInvocationHandler implements InvocationHandler {

    private String serviceName;

    private final RpcClient rpcClient;

    private final ServiceRegistry serviceRegistry;

    private final ServiceInstanceSelector selector;

    private final List<RequestInterceptor> interceptors;

    public ServiceInvocationHandler(String serviceName, RpcClient rpcClient, List<RequestInterceptor> interceptors) {
        this.serviceName = serviceName;
        this.rpcClient = rpcClient;
        this.serviceRegistry = rpcClient.getServiceRegistry();
        this.selector = rpcClient.getSelector();
        this.interceptors = interceptors;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isObjectDeclaredMethod(method)) {
            // 本地处理
            return handleObjectMethod(proxy, method, args);
        }

        // 在 RPC 服务集群中选择其中一个实例（负载均衡）
        ServiceInstance serviceInstance = selectServiceProviderInstance();

        // 非 Object 方法实行远程调用
        InvocationRequest request = createRequest(method, args);
        Object result = null;
        for (RequestInterceptor interceptor : this.interceptors) {
            if (!interceptor.support(method, serviceInstance, serviceName, request))
                continue;

            if (!interceptor.beforeExecute(method, serviceInstance, request)) {
                triggerComplete(method, serviceInstance, request, result, null);
                return result;
            }
        }
        try {
            result = execute(request, serviceInstance, proxy);
            triggerComplete(method, serviceInstance, request, result, null);
        } catch (Exception ex) {
            triggerComplete(method, serviceInstance, request, result, ex);
        }
        return result;
    }

    private void triggerComplete(Method method, ServiceInstance serviceInstance, InvocationRequest request, Object result, Exception ex) {
        for (RequestInterceptor interceptor : this.interceptors) {
            try {
                interceptor.afterExecute(method, serviceInstance, request, result, ex);
            } catch (Exception ignored) {
                //todo 异常日志记录
            }
        }

    }

    private Object execute(InvocationRequest request, ServiceInstance serviceInstance, Object proxy) {
        // 与目标 RPC 服务器建联
        ChannelFuture channelFuture = rpcClient.connect(serviceInstance);
        // 发送请求（消息），关联 requestId
        sendRequest(request, channelFuture);
        // 创建请求对应的 Future 对象
        ExchangeFuture exchangeFuture = createExchangeFuture(request);

        try {
            // 阻塞 RPC 服务器响应，直到对方将 Response（对应 requestId) 设置到 ExchangeFuture 所关联的 Promise
            // 即 Promise#setSuccess 或 Promise#setFailure 被调用
            // 参考：InvocationResponseHandler
            return exchangeFuture.get();
        } catch (Exception e) {
            removeExchangeFuture(request.getRequestId());
        }

        throw new IllegalStateException("Invocation failed!");
    }

    private void sendRequest(InvocationRequest request, ChannelFuture channelFuture) {
        channelFuture.channel().writeAndFlush(request);
    }

    private ServiceInstance selectServiceProviderInstance() {
        List<ServiceInstance> serviceInstances = serviceRegistry.getServiceInstances(serviceName);
        return selector.select(serviceInstances);
    }

    private InvocationRequest createRequest(Method method, Object[] args) {
        InvocationRequest request = new InvocationRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setServiceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        // TODO
        request.setMetadata(new HashMap<>());
        return request;
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        switch (methodName) {
            case "equals":
                // TODO
                break;
            case "hashCode":
                // TODO
                break;
            case "toString":
                // TODO
                break;
        }
        return null;
    }

    private boolean isObjectDeclaredMethod(Method method) {
        return Object.class == method.getDeclaringClass();
    }
}
