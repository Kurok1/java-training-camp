package indi.kurok1.curator.config;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class CuratorClientConfig {

    private String globalPath;

    private String connectString;

    private String namespace;

    private int sessionTimeoutMs;

    private int connectionTimeoutMs;

    public String getGlobalPath() {
        return globalPath;
    }

    public void setGlobalPath(String globalPath) {
        this.globalPath = globalPath;
    }

    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
}
