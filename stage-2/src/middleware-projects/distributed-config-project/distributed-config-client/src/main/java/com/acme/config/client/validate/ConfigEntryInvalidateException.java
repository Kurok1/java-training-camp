package com.acme.config.client.validate;

/**
 * 配置校验失败异常
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ConfigEntryInvalidateException extends RuntimeException {

    public ConfigEntryInvalidateException(String message) {
        super(message);
    }
}
