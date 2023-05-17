package com.acme.config.client.validate.impl;

import com.acme.config.client.validate.ConfigEntryInvalidateException;
import com.acme.config.client.validate.ConfigValidator;
import com.acme.config.common.ConfigEntry;
import com.acme.config.common.util.MD5Utils;
import org.springframework.util.ObjectUtils;

/**
 * 校验md5是否一致
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class MD5ConfigValidator implements ConfigValidator {

    @Override
    public void validate(ConfigEntry configEntry) throws ConfigEntryInvalidateException {
        String md5FromServer = configEntry.getContentMd5();
        if (ObjectUtils.isEmpty(md5FromServer))
            throw new ConfigEntryInvalidateException("服务端配置未传递md5值");

        String md5 = MD5Utils.generateMD5(configEntry.getContent());
        if (!md5FromServer.equals(md5))
            throw new ConfigEntryInvalidateException("服务端配置传递md5值与实际不符， 服务端：" + md5FromServer + ", 当前：" + md5);
    }
}
