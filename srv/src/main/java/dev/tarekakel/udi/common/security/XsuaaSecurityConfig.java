package dev.tarekakel.udi.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cloud Foundry: stateless JWT resource server. The approuter already enforces scopes at the edge;
 * the service enforces them again so a caller that bypasses the router gains nothing.
 */
@Configuration
@Profile("cloud")
class XsuaaSecurityConfig {

    @Bean
    SecurityFilterChain xsuaaApi(HttpSecurity http, XsuaaProperties xsuaa) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/assistant/ask").hasAnyAuthority(Scopes.VIEWER, Scopes.EDITOR)
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority(Scopes.VIEWER, Scopes.EDITOR)
                        .requestMatchers("/api/**").hasAuthority(Scopes.EDITOR)
                        .requestMatchers("/mcp", "/mcp/**").hasAnyAuthority(Scopes.VIEWER, Scopes.EDITOR)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter(xsuaa))));
        return http.build();
    }

    /** Signature via XSUAA's JWK set, standard timestamp checks, plus an audience check for this application. */
    @Bean
    JwtDecoder jwtDecoder(XsuaaProperties xsuaa) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(xsuaa.jwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new XsuaaAudienceValidator(xsuaa)));
        return decoder;
    }

    private static Converter<Jwt, AbstractAuthenticationToken> authenticationConverter(XsuaaProperties xsuaa) {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new XsuaaScopeAuthoritiesConverter(xsuaa.xsappname()));
        return converter;
    }
}
