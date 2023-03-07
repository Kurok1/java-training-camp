package com.alipay.sofa.jraft.example.counter.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.example.counter.CounterClosure;
import com.alipay.sofa.jraft.example.counter.CounterService;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class DecrementAndGetRequestProcessor implements RpcProcessor<CounterOutter.DecrementAndGetRequest> {

    private final CounterService counterService;

    public DecrementAndGetRequestProcessor(final CounterService counterService) {
        super();
        this.counterService = counterService;
    }

    @Override
    public void handleRequest(RpcContext rpcContext, CounterOutter.DecrementAndGetRequest request) {
        final CounterClosure closure = new CounterClosure() {
            @Override
            public void run(Status status) {
                rpcContext.sendResponse(getValueResponse());
            }
        };

        this.counterService.decrementAndGet(request.getDelta(), closure);
    }

    @Override
    public String interest() {
        return CounterOutter.DecrementAndGetRequest.class.getName();
    }
}
