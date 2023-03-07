package com.alipay.sofa.jraft.example.election;

import com.alipay.sofa.jraft.entity.LeaderChangeContext;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public interface FollowerStateListener {

    /**
     * 开始follower指定leaderId
     * @param context leader变更上下文
     */
    void onStartFollowing(LeaderChangeContext context);

    /**
     * 绑定的leader变更
     * @param context leader变更上下文
     */
    void onStopFollowing(LeaderChangeContext context);

}
