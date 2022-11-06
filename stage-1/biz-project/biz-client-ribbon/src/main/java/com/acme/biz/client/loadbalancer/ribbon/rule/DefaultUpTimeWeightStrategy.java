package com.acme.biz.client.loadbalancer.ribbon.rule;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

/**
 * default implements for {@link UpTimeWeightStrategy},base on {@link ChronoUnit#MILLIS}
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class DefaultUpTimeWeightStrategy implements UpTimeWeightStrategy {

    public final TemporalUnit MILLIS_UNIT = ChronoUnit.SECONDS;

    private final long RANGE_INIT = 30;
    private final long RANGE_NORMAL = 10 * RANGE_INIT;

    /**
     * [0,1000) return 1.0
     * [1000, 30*1000) return 10.0
     * [30*1000, +) return 100
     */

    @Override
    public double getWeight(Duration duration) {
        long value = duration.get(MILLIS_UNIT);
        if (value < RANGE_INIT)
            return 1.0;
        else if (value < RANGE_NORMAL) {
            return 10.0;
        } else return NORMAL_WEIGHT;
    }
}
