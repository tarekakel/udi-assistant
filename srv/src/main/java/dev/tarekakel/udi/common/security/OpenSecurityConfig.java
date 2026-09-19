package dev.tarekakel.udi.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** Local development and tests: no login, identity comes from the {@code X-User} header. Never active on BTP. */
@Configuration
@Profile("!cloud")
class OpenSecurityConfig {

    @Bean
    SecurityFilterChain openApi(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frames -> frames.sameOrigin()))   // H2 console uses frames
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}
