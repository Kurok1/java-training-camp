package com.acme.config.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ConfigEntry implements Comparable<ConfigEntry> {

    private String configId;

    private String configType;

    private String content;

    @JsonIgnore
    private Map<String, String> contentMap;

    private String contentMd5;

    private int order = 100;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updated;

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public Map<String, String> getContentMap() {
        return Collections.unmodifiableMap(this.contentMap);
    }

    public void setContentMap(Map<String, String> contentMap) {
        this.contentMap = contentMap;
    }

    @JsonIgnore
    public String getValue(String key) {
        if (this.contentMap == null)
            return null;

        return this.contentMap.get(key);
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void applyChanges(Collection<ConfigWatcher.ChangedConfigEntry> changedConfigs) {
        if (changedConfigs == null || changedConfigs.isEmpty())
            return;

        for (ConfigWatcher.ChangedConfigEntry configEntry : changedConfigs) {
            String key = configEntry.getKey();
            String value = configEntry.getNewValue();
            this.contentMap.put(key, value);
        }
    }

    @Override
    public int compareTo(ConfigEntry o) {
        if (o == null)
            return 1;

        if (equals(o))
            return 0;

        return Integer.compare(this.order, o.getOrder());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigEntry that = (ConfigEntry) o;

        if (!configId.equals(that.configId)) return false;
        return configType.equals(that.configType);
    }

    @Override
    public int hashCode() {
        int result = configId.hashCode();
        result = 31 * result + configType.hashCode();
        return result;
    }
}
