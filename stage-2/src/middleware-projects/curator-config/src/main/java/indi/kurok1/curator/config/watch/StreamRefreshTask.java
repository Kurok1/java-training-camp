package indi.kurok1.curator.config.watch;

import indi.kurok1.curator.config.source.ZookeeperConfigUtils;
import indi.kurok1.curator.config.stream.StreamConfigAccumulator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class StreamRefreshTask implements Runnable {

    private final RestTemplate restTemplate;
    private final String configUrl;
    private final StreamConfigAccumulator accumulator;
    private final ApplicationEventPublisher publisher;
    private final String path;
    private final Environment environment;
    private MutablePropertySources propertySources;

    public StreamRefreshTask(RestTemplate restTemplate, String configUrl, StreamConfigAccumulator accumulator, ApplicationEventPublisher publisher, ConfigurableEnvironment environment, String path) {
        this.restTemplate = restTemplate;
        this.configUrl = configUrl;
        this.accumulator = accumulator;
        this.publisher = publisher;
        this.path = path;
        this.environment = environment;
        this.propertySources = environment.getPropertySources();
    }

    @Override
    public void run() {
        restTemplate.execute(configUrl, HttpMethod.GET, request -> {}, this::handleResponse);
    }

    protected ClientHttpResponse handleResponse(ClientHttpResponse response) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody()));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                PropertySource<?> previous = propertySources.get(this.path);
                PropertySource<?> propertySource = this.accumulator.update(line, previous);

                //比较差异
                Collection<String> diffKeys = ZookeeperConfigUtils.compareDiff(propertySource, environment);
                //更新配置
                if (previous == null)
                    this.propertySources.addFirst(propertySource);
                else this.propertySources.replace(this.path, propertySource);
                ZookeeperConfigChangedEvent event = new ZookeeperConfigChangedEvent(this.path, diffKeys);
                this.publisher.publishEvent(event);
            }
        } catch (IOException e) {
            //Something clever
        }


        return response;
    }
}
