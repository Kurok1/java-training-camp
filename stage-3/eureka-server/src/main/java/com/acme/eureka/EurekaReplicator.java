package com.acme.eureka;

import com.netflix.appinfo.InstanceInfo;

import java.io.IOException;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@FunctionalInterface
public interface EurekaReplicator {

    byte ADD = 0;
    byte HEART_BEAT = 1;

    byte DELETE = 2;

    void replicate(InstanceInfo instance, byte action) throws IOException;

}
