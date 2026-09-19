package dev.tarekakel.udi.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Deliberately not a JpaRepository: the trail can be appended and read, never updated or deleted. */
interface AuditEntryRepository extends Repository<AuditEntry, UUID> {

    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByEntityTypeAndEntityIdOrderByPerformedAtAsc(String entityType, String entityId);
}
