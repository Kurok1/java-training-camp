package com.acme.config.common;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public enum ConfigKind {
    JSON("JSON", "application/json");

    private final String name;

    private final String mediaType;

    ConfigKind(String name, String mediaType) {
        this.name = name;
        this.mediaType = mediaType;
    }

    public String getName() {
        return name;
    }

    public String getMediaType() {
        return mediaType;
    }
}
