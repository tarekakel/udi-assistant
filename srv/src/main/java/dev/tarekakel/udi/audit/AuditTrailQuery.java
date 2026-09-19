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

    /** Newest first, across all entities; what a reviewer opens to see "what happened recently". */
    public List<AuditEntryResponse> recent() {
        return entries.findTop200ByOrderByPerformedAtDesc().stream().map(AuditEntryResponse::from).toList();
    }
}
