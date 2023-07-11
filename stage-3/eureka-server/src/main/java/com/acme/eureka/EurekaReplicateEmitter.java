package com.acme.eureka;

import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author <a href="mailto:hanchao@66yunlian.com">韩超</a>
 * @since 1.0.0
 */
@Component
public class EurekaReplicateEmitter implements SmartApplicationListener {

    private final EurekaReplicator replicator;

    public EurekaReplicateEmitter(EurekaReplicator replicator) {
        this.replicator = replicator;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return EurekaInstanceRegisteredEvent.class.isAssignableFrom(eventType)
                || EurekaInstanceRenewedEvent.class.isAssignableFrom(eventType)
                || EurekaInstanceCanceledEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        try {
            if (event instanceof EurekaInstanceRegisteredEvent) {
                onRegister((EurekaInstanceRegisteredEvent) event);
            }
            if (event instanceof EurekaInstanceCanceledEvent) {
                onDeregister((EurekaInstanceCanceledEvent) event);
            }
            if (event instanceof EurekaInstanceRenewedEvent) {
                onRenew((EurekaInstanceRenewedEvent) event);
            }
        } catch (Exception ignored) {

        }
    }

    private void onRegister(EurekaInstanceRegisteredEvent event) throws IOException {
        if (event.isReplication())
            return;
        InstanceInfo instanceInfo = event.getInstanceInfo();
        this.replicator.replicate(instanceInfo, EurekaReplicator.ADD);
    }

    private void onDeregister(EurekaInstanceCanceledEvent event) throws IOException {
        if (event.isReplication())
            return;
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName(event.getAppName())
                .setInstanceId(event.getServerId())
                .build();
        this.replicator.replicate(instanceInfo, EurekaReplicator.DELETE);
    }

    private void onRenew(EurekaInstanceRenewedEvent event) throws IOException {
        if (event.isReplication())
            return;

        InstanceInfo instanceInfo = event.getInstanceInfo();
        this.replicator.replicate(instanceInfo, EurekaReplicator.HEART_BEAT);
    }
}
