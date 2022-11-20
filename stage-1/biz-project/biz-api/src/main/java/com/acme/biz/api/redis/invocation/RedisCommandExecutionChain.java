package com.acme.biz.api.redis.invocation;

/**
 * Redis Command调用链路
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public interface RedisCommandExecutionChain {

    Object execute(RedisCommandExecutionContext executionContext);

}
