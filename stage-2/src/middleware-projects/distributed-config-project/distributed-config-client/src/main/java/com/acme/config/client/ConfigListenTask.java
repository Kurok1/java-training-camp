package com.acme.config.client;

import com.acme.config.common.ConfigWatcher;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class ConfigListenTask implements Runnable {

    private final ClientConfiguration clientConfiguration;
    private final ConfigWatcherDispatcher dispatcher;
    private final RestTemplate restTemplate;

    private final ObjectMapper JSON = new ObjectMapper();

    private static final String WATCH_URI = "/config/watch/";

    public ConfigListenTask(ClientConfiguration clientConfiguration, ConfigWatcherDispatcher dispatcher, RestTemplate restTemplate) {
        this.clientConfiguration = clientConfiguration;
        this.dispatcher = dispatcher;
        this.restTemplate = restTemplate;
        JSON.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public void run() {
        final String url = this.clientConfiguration.getRemoteConfigServer() + WATCH_URI + clientConfiguration.getClientId();
        this.restTemplate.execute(url, HttpMethod.GET, request -> {}, this::onMessage);
    }

    private ClientHttpResponse onMessage(ClientHttpResponse response) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody()));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    response.close();
                    break;
                }

                line = line.trim();
                //处理：
                int index = line.indexOf(":");
                if (index == -1)
                    continue;
                line = line.substring(index + 1);
                if (ObjectUtils.isEmpty(line))
                    continue;
                ChangeData data = JSON.readValue(line, ChangeData.class);
                this.dispatcher.onChange(data.getConfigId(), data.getChanges());
            }
        } catch (IOException e) {
            //Something clever
            response.close();
        }
        return response;
    }

    @JsonRootName("data")
    public static class ChangeData {
        private String configId;

        private Collection<ConfigWatcher.ChangedConfigEntry> changes;

        public String getConfigId() {
            return configId;
        }

        public void setConfigId(String configId) {
            this.configId = configId;
        }

        public Collection<ConfigWatcher.ChangedConfigEntry> getChanges() {
            return changes;
        }

        public void setChanges(Collection<ConfigWatcher.ChangedConfigEntry> changes) {
            this.changes = changes;
        }
    }
}
