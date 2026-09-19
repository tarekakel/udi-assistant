package dev.tarekakel.udi.audit;

/**
 * Published by an application service after a business change. The audit module observes it;
 * the publishing module never depends on how or where the trail is stored.
 */
public record EntityChangedEvent(String entityType, String entityId, AuditAction action, ChangeSet changes,
                                 String reason, String performedBy) {
}
