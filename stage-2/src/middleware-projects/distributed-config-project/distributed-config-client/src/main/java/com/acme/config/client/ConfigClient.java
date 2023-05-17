package com.acme.config.client;

import com.acme.config.client.validate.ConfigEntryInvalidateException;
import com.acme.config.client.validate.ConfigValidator;
import com.acme.config.common.*;
import com.acme.config.common.util.resolve.ConfigResolvers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
class ConfigClient implements ConfigManager, ConfigWatcher, AutoCloseable {

    /**
     * 配置校验
     */
    private final List<ConfigValidator> validators = ConfigValidator.loadDefaults();
    private final ClientConfiguration clientConfiguration;
    private final LocalConfigService configService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConfigEntrySet configEntrySet = new ConfigEntrySet();

    /**
     * urls
     */
    private static final String GET_URI = "/config/";

    ConfigClient(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        this.configService = new LocalConfigService(clientConfiguration, configEntrySet, restTemplate);
        init(configService);
    }


    @Override
    public ConfigEntry getConfig(String configId) {
        ConfigEntry cache = this.configEntrySet.getConfigEntryById(configId);
        if (cache != null)
            return cache;

        //从远端服务获取
        final String url = this.clientConfiguration.getRemoteConfigServer() + GET_URI + configId;
        ResponseEntity<ConfigEntry> response = this.restTemplate.getForEntity(url, ConfigEntry.class);
        if (!response.getStatusCode().is2xxSuccessful())
            throw new RuntimeException("get config fail : " + configId);
        ConfigEntry entry = response.getBody();
        if (entry == null)
            throw new IllegalArgumentException("not found config : " + configId);

        for (ConfigValidator validator : this.validators) {
            try {
                validator.validate(entry);
            } catch (ConfigEntryInvalidateException e) {
                throw new IllegalArgumentException(e);
            }
        }
        //本地指定
        ConfigKind kind = this.clientConfiguration.getKind(configId);
        if (kind == null)
            //服务端指定
            kind = ConfigKind.valueOf(entry.getConfigType());
        final Map<String, String> contentMap = ConfigResolvers.resolveConfigContent(kind, entry.getContent());
        entry.setContentMap(contentMap);
        this.configEntrySet.addConfigEntry(entry);
        this.configService.registerWatcher(configId, this);
        return entry;
    }

    @Override
    public void saveConfig(ConfigEntry configEntry) {
        //客户端暂时不支持修改远程服务配置
        System.err.println("客户端暂时不支持修改远程服务配置");
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConfig(String configId) {
        //客户端暂时不支持修改远程服务配置
        System.err.println("客户端暂时不支持修改远程服务配置");
        throw new UnsupportedOperationException();
    }

    protected void init(LocalConfigService configService) {
        //初始化配置
        Collection<String> configIds = this.clientConfiguration.interestedConfigs();
        for (String configId : configIds)
            try {
                getConfig(configId);
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    @Override
    public void onChange(String configId, Collection<ChangedConfigEntry> changedConfigs) {
        ConfigEntry configEntry = this.configEntrySet.getConfigEntryById(configId);
        if (configEntry == null)
            return;
        configEntry.applyChanges(changedConfigs);

    }

    public ConfigService getConfigService() {
        return configService;
    }

    @Override
    public void close() throws Exception {
        this.configService.close();
    }
}
