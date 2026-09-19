package dev.tarekakel.udi.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credentials of the bound XSUAA instance, mapped from {@code VCAP_SERVICES} by Spring Boot on Cloud Foundry. */
@ConfigurationProperties(prefix = "app.security.xsuaa")
public record XsuaaProperties(String url, String xsappname, String clientId) {

    /** XSUAA publishes its signing keys as a JWK set under this path. */
    public String jwkSetUri() {
        return url + "/token_keys";
    }
}
