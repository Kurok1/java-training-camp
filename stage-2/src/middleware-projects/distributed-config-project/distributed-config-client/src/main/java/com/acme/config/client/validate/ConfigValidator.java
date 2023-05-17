package com.acme.config.client.validate;

import com.acme.config.common.ConfigEntry;

import java.util.*;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public interface ConfigValidator {

    void validate(ConfigEntry configEntry) throws ConfigEntryInvalidateException;

    static List<ConfigValidator> loadDefaults() {
        ServiceLoader<ConfigValidator> validatorLoader = ServiceLoader.load(ConfigValidator.class);
        Iterator<ConfigValidator> iterator = validatorLoader.iterator();
        if (!iterator.hasNext())
            return Collections.emptyList();

        List<ConfigValidator> list = new ArrayList<>();
        while (iterator.hasNext())
            list.add(iterator.next());

        return list;
    }

}
