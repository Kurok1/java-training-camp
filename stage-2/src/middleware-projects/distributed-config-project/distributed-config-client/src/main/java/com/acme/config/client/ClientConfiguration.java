package com.acme.config.client;

import com.acme.config.common.ConfigKind;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ClientConfiguration {

    private final String clientId;

    private final String remoteConfigServer;

    private final Map<String, ConfigKind> configKindMap = new HashMap<>();

    private ClientConfiguration(String clientId, String remoteConfigServer) {
        this.clientId = clientId;
        this.remoteConfigServer = remoteConfigServer;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRemoteConfigServer() {
        return remoteConfigServer;
    }


    protected void addKind(String configId, ConfigKind kind) {
        this.configKindMap.put(configId, kind);
    }

    public ConfigKind getKind(String configId) {
        return this.configKindMap.get(configId);
    }

    public Collection<String> interestedConfigs() {
        return Collections.unmodifiableCollection(this.configKindMap.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientConfiguration that = (ClientConfiguration) o;

        return clientId.equals(that.clientId);
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }

    public static class Builder {
        private final Map<String, ConfigKind> configKindMap = new HashMap<>();

        public Builder interest(String configId) {
            this.configKindMap.put(configId, null);
            return this;
        }

        public Builder interest(String configId, ConfigKind configKind) {
            this.configKindMap.put(configId, configKind);
            return this;
        }

        public ClientConfiguration build(String clientId, String remoteConfigServer) {
            ClientConfiguration configuration = new ClientConfiguration(clientId, remoteConfigServer);
            configKindMap.forEach(configuration::addKind);
            return configuration;
        }

    }
}
