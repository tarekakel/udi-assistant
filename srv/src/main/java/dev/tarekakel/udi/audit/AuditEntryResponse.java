package dev.tarekakel.udi.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(UUID id, String entityType, String entityId, AuditAction action, String field,
                                 String oldValue, String newValue, String reason, String performedBy, Instant performedAt) {

    static AuditEntryResponse from(AuditEntry entry) {
        return new AuditEntryResponse(entry.getId(), entry.getEntityType(), entry.getEntityId(), entry.getAction(),
                entry.getFieldName(), entry.getOldValue(), entry.getNewValue(), entry.getReason(),
                entry.getPerformedBy(), entry.getPerformedAt());
    }
}
