package com.acme.middleware.rpc.client;

import com.acme.middleware.rpc.InvocationRequest;
import com.acme.middleware.rpc.service.ServiceInstance;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;

import java.lang.reflect.Method;

/**
 * 客户端传播xid
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class SeataPropagationInterceptor implements RequestInterceptor {

    @Override
    public boolean support(Method method, ServiceInstance serviceInstance, String serviceName, InvocationRequest request) {
        return true;
    }

    @Override
    public boolean beforeExecute(Method method, ServiceInstance serviceInstance, InvocationRequest request) throws Exception {
        String xid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();
        if (xid != null && branchType != null) {
            request.getMetadata().put(RootContext.KEY_XID, xid);
            request.getMetadata().put(RootContext.KEY_BRANCH_TYPE, branchType.name());
        }

        return true;
    }

    @Override
    public void afterExecute(Method method, ServiceInstance serviceInstance, InvocationRequest request, Object result, Exception ex) throws Exception {
        request.getMetadata().remove(RootContext.KEY_XID);
        request.getMetadata().remove(RootContext.KEY_BRANCH_TYPE);
    }
}
