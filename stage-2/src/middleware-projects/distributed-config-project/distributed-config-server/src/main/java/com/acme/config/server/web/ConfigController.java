package com.acme.config.server.web;

import com.acme.config.common.ConfigEntry;
import com.acme.config.common.ConfigManager;
import com.acme.config.server.watcher.SseEmitterServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@RestController
@RequestMapping("config")
public class ConfigController {


    @Autowired
    private ConfigManager configService;

    @GetMapping("/{id}")
    public ResponseEntity<ConfigEntry> getConfigById(@PathVariable("id") String id) {
        ConfigEntry configEntry = configService.getConfig(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(configEntry);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ConfigEntry configEntry) {
        this.configService.saveConfig(configEntry);
        return ResponseEntity.ok("ok");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") String id, @RequestBody ConfigEntry configEntry) {
        configEntry.setConfigId(id);
        this.configService.saveConfig(configEntry);
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") String id) {
        this.configService.deleteConfig(id);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/watch/{clientId}")
    public SseEmitter watchConfig(@PathVariable("clientId") String clientId) {
        return SseEmitterServer.onConnect(clientId);
    }

}
