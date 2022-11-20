package com.acme.biz.api.redis.invocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * default implements for {@link RedisCommandExecutionChain}
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class DefaultRedisCommandExecutionChain implements RedisCommandExecutionChain {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRedisCommandExecutionChain.class);

    private final List<RedisCommandInterceptor> interceptorList;

    private AtomicInteger index = new AtomicInteger(0);

    public DefaultRedisCommandExecutionChain(List<RedisCommandInterceptor> interceptorList) {
        this.interceptorList = interceptorList;
    }

    @Override
    public Object execute(RedisCommandExecutionContext executionContext) {
        if (CollectionUtils.isEmpty(interceptorList)) {
            //no interceptor
            return executeInternal(executionContext);
        }

        final int currentIndex = index.get();
        if (currentIndex < interceptorList.size()) {
            RedisCommandInterceptor interceptor = interceptorList.get(currentIndex);
            if (interceptor == null)
                throw new NullPointerException();

            index.incrementAndGet();
            return interceptor.execute(executionContext, this);
        }

        return executeInternal(executionContext);
    }

    private Object executeInternal(RedisCommandExecutionContext executionContext) {
        try {
            executionContext.start();
            Object result = executionContext.execute();
            executionContext.setResult(result);
            executionContext.finish();
            return result;
        } catch (Exception e) {
            //调用失败
            logger.error(e.getMessage(), e);
            executionContext.setResult(null);
            executionContext.finish();
            executionContext.setException(e);
            return null;
        }
    }
}
