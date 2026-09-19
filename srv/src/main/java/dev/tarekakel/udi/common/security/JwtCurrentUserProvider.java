package dev.tarekakel.udi.common.security;

import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Cloud profile: the acting user is whoever XSUAA authenticated. Prefers the human-readable
 * {@code user_name}, falls back to {@code email}, then to the OAuth client for technical callers.
 */
@Component
@Profile("cloud")
class JwtCurrentUserProvider implements CurrentUserProvider {

    private static final String[] IDENTITY_CLAIMS = {"user_name", "email", "client_id"};

    @Override
    public String currentUser() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            return SYSTEM;
        }
        Jwt jwt = token.getToken();
        return Stream.of(IDENTITY_CLAIMS)
                .map(jwt::getClaimAsString)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(token.getName());
    }
}
