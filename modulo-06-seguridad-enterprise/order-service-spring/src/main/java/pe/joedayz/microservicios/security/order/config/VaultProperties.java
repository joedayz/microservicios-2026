package pe.joedayz.microservicios.security.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("module6.vault")
public class VaultProperties {

    private boolean enabled;
    private String address = "http://localhost:8200";
    private String token = "root";
    private String kvMount = "secret";
    private String path = "module6/inventory-client";
    private String field = "client-id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getKvMount() {
        return kvMount;
    }

    public void setKvMount(String kvMount) {
        this.kvMount = kvMount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
