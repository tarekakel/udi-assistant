package dev.tarekakel.udi.common.persistence;

import dev.tarekakel.udi.common.security.CurrentUserProvider;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Wires Spring Data auditing (createdBy / lastModifiedBy) to the same user strategy the audit trail uses. */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class JpaAuditingConfig {

    @Bean
    AuditorAware<String> auditorAware(CurrentUserProvider users) {
        return () -> Optional.of(users.currentUser());
    }
}
