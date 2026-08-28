package pe.joedayz.microservicios.security.order.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "module7")
public class GatewayTrafficProperties {

    private final Traffic traffic = new Traffic();
    private final Waf waf = new Waf();

    public Traffic getTraffic() {
        return traffic;
    }

    public Waf getWaf() {
        return waf;
    }

    public static class Traffic {

        private Duration defaultTimeout = Duration.ofSeconds(2);
        private Duration orderTimeout = Duration.ofSeconds(3);
        private Duration inventoryTimeout = Duration.ofSeconds(2);
        private Duration hedgeDelay = Duration.ofMillis(250);

        public Duration getDefaultTimeout() {
            return defaultTimeout;
        }

        public void setDefaultTimeout(Duration defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }

        public Duration getOrderTimeout() {
            return orderTimeout;
        }

        public void setOrderTimeout(Duration orderTimeout) {
            this.orderTimeout = orderTimeout;
        }

        public Duration getInventoryTimeout() {
            return inventoryTimeout;
        }

        public void setInventoryTimeout(Duration inventoryTimeout) {
            this.inventoryTimeout = inventoryTimeout;
        }

        public Duration getHedgeDelay() {
            return hedgeDelay;
        }

        public void setHedgeDelay(Duration hedgeDelay) {
            this.hedgeDelay = hedgeDelay;
        }
    }

    public static class Waf {

        private boolean enabled = true;
        private List<String> blockedUserAgents = new ArrayList<>();
        private List<String> blockedPathFragments = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getBlockedUserAgents() {
            return blockedUserAgents;
        }

        public void setBlockedUserAgents(List<String> blockedUserAgents) {
            this.blockedUserAgents = blockedUserAgents;
        }

        public List<String> getBlockedPathFragments() {
            return blockedPathFragments;
        }

        public void setBlockedPathFragments(List<String> blockedPathFragments) {
            this.blockedPathFragments = blockedPathFragments;
        }
    }
}
