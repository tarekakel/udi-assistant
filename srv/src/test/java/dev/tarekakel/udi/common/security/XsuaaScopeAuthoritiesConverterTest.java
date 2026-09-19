package dev.tarekakel.udi.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class XsuaaScopeAuthoritiesConverterTest {

    private final XsuaaScopeAuthoritiesConverter converter = new XsuaaScopeAuthoritiesConverter("udi-assistant!t1");

    @Test
    void keepsOnlyThisApplicationsScopesWithoutThePrefix() {
        Jwt jwt = jwt(List.of("udi-assistant!t1.Editor", "openid", "other-app!t9.Viewer", "udi-assistant!t1.Viewer"));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("Editor", "Viewer");
    }

    @Test
    void toleratesTokensWithoutScopes() {
        assertThat(converter.convert(jwt(null))).isEmpty();
    }

    @Test
    void audienceValidatorAcceptsOwnClientAndRejectsOthers() {
        var validator = new XsuaaAudienceValidator(new XsuaaProperties("https://x", "udi-assistant!t1", "sb-udi-assistant!t1"));

        assertThat(validator.validate(jwtWithAudience(List.of("openid", "udi-assistant!t1"))).hasErrors()).isFalse();
        assertThat(validator.validate(jwtWithAudience(List.of("someone-else!t2"))).hasErrors()).isTrue();
    }

    private static Jwt jwt(List<String> scopes) {
        Jwt.Builder builder = baseJwt();
        if (scopes != null) {
            builder.claim("scope", scopes);
        }
        return builder.build();
    }

    private static Jwt jwtWithAudience(List<String> audience) {
        return baseJwt().audience(audience).build();
    }

    private static Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("user")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300));
    }
}
