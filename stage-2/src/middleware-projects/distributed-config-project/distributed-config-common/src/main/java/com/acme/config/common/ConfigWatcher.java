package com.acme.config.common;

import java.util.Collection;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@FunctionalInterface
public interface ConfigWatcher {

    void onChange(String configId, Collection<ChangedConfigEntry> changedConfigs);


    class ChangedConfigEntry {
        private String key;

        private String oldValue;

        private String newValue;

        public ChangedConfigEntry() {
        }

        public ChangedConfigEntry(String key, String oldValue, String newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getKey() {
            return key;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }
    }
}
