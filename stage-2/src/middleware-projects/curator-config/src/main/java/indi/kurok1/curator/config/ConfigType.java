package indi.kurok1.curator.config;

/**
 * 配置类型
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public enum ConfigType {
    JSON("application/json", "json"),
    XML("application/xml", "xml"),
    TEXT("text/html", "text"),
    EVENT_STREAM("text/event-stream", "event-stream")
    ;
    private final String contentType;
    private final String name;

    ConfigType(String contentType, String name) {
        this.contentType = contentType;
        this.name = name;
    }
}
