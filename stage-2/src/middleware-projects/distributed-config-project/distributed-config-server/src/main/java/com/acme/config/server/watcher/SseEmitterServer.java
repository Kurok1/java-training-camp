package com.acme.config.server.watcher;

import com.acme.config.common.ConfigWatcher;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class SseEmitterServer implements ConfigWatcher {

    private static final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();


    public static SseEmitter onConnect(String clientId) {
        //设置超时时间，0表示不过期。默认30秒，超过时间未完成会抛出异常：AsyncRequestTimeoutException
        SseEmitter sseEmitter = new SseEmitter(0L);
        //注册回调
        sseEmitter.onCompletion(() -> emitterMap.remove(clientId));
        sseEmitter.onError((t) -> emitterMap.remove(clientId));
        sseEmitter.onTimeout(() -> emitterMap.remove(clientId));
        emitterMap.put(clientId, sseEmitter);
        
        return sseEmitter;
    }

    private static void sendToClient(String configId, Collection<ChangedConfigEntry> changedConfigs) {
        Map<String, Object> map = new HashMap<>();
        map.put("configId", configId);
        map.put("changes", changedConfigs);
        emitterMap.forEach((clientId, sseEmitter) -> {
            try {
                sseEmitter.send(map, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                emitterMap.remove(clientId);
            }
        });
    }

    @Override
    public void onChange(String configId, Collection<ChangedConfigEntry> changedConfigs) {
        sendToClient(configId, changedConfigs);
    }
}
