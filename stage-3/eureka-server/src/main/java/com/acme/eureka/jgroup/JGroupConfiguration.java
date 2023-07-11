package com.acme.eureka.jgroup;

import com.acme.eureka.EurekaReplicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@Configuration
public class JGroupConfiguration {

    @Bean
    public EurekaReplicator node() {
        return new JGroupNode("eureka-server-cluster");
    }

}
