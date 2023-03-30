package indi.kurok1.curator.config.stream;

import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * 输入流解析器
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public abstract class StreamConfigAccumulator {

    protected final String name;

    public StreamConfigAccumulator(String name) {
        this.name = name;
    }

    public abstract PropertySource<?> update(String value, @Nullable PropertySource<?> previous) throws IOException;

}
