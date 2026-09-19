package dev.tarekakel.udi.audit;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the trail; the only public entry point other modules use. */
@Service
@Transactional(readOnly = true)
public class AuditTrailQuery {

    private final AuditEntryRepository entries;

    AuditTrailQuery(AuditEntryRepository entries) {
        this.entries = entries;
    }

    public List<AuditEntryResponse> forEntity(String entityType, String entityId) {
        return entries.findByEntityTypeAndEntityIdOrderByPerformedAtAsc(entityType, entityId).stream()
                .map(AuditEntryResponse::from)
                .toList();
    }
}
