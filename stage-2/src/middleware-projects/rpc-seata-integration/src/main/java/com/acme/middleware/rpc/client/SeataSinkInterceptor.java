package com.acme.middleware.rpc.client;

import com.acme.middleware.rpc.InvocationRequest;
import com.acme.middleware.rpc.InvocationResponse;
import com.acme.middleware.rpc.server.RpcInvokeInterceptor;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;

/**
 * 服务端处理程序
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class SeataSinkInterceptor implements RpcInvokeInterceptor {

    @Override
    public void beforeInvoke(Object service, InvocationRequest request) {
        String rpcXid = getMetaData(request, RootContext.KEY_XID);
        String rpcBranchType = getMetaData(request, RootContext.KEY_BRANCH_TYPE);

        if (!StringUtils.isEmpty(rpcXid) && !StringUtils.isEmpty(rpcBranchType)) {
            //绑定当前事物
            RootContext.bind(rpcXid);
            if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType)) {
                RootContext.bindBranchType(BranchType.TCC);
            }
        }
    }

    @Override
    public void afterInvoke(Object service, InvocationRequest request, InvocationResponse response) {
        BranchType previousBranchType = RootContext.getBranchType();
        String unbindXid = RootContext.unbind();
        String rpcXid = getMetaData(request, RootContext.KEY_XID);
        if (BranchType.TCC == previousBranchType) {
            RootContext.unbindBranchType();
        }

        if (!rpcXid.equalsIgnoreCase(unbindXid)) {
            //xid changed
            if (unbindXid != null && previousBranchType != null) {
                RootContext.bind(unbindXid);
                if (BranchType.TCC == previousBranchType) {
                    RootContext.bindBranchType(BranchType.TCC);
                }
            }
        }
    }

    private String getMetaData(InvocationRequest request, String key) {
        return request.getMetadata().getOrDefault(key, "").toString();
    }
}
