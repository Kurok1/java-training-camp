package com.acme.biz.client.loadbalancer.ribbon.rule;

import java.time.Duration;
import java.time.temporal.TemporalUnit;

/**
 * 基于uptime决定权重
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@FunctionalInterface
public interface UpTimeWeightStrategy {

    double getWeight(Duration duration);

    double NORMAL_WEIGHT = 100.0;

}
