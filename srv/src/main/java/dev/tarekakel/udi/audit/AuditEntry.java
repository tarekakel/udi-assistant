package dev.tarekakel.udi.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One immutable line of the audit trail (who / when / what / why). No setters and an append-only repository
 * keep the trail tamper-evident at the application level.
 */
@Entity
@Table(name = "audit_entry")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)   // VARCHAR(36) everywhere: H2 and SAP HANA agree, no vendor UUID type
    private UUID id;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 64)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditAction action;

    @Column(name = "field_name", updatable = false, length = 50)
    private String fieldName;

    @Column(name = "old_value", updatable = false, length = 1000)
    private String oldValue;

    @Column(name = "new_value", updatable = false, length = 1000)
    private String newValue;

    @Column(updatable = false, length = 500)
    private String reason;

    @Column(name = "performed_by", nullable = false, updatable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    protected AuditEntry() {
    }

    static AuditEntry of(EntityChangedEvent event, FieldChange change, Instant at) {
        var entry = new AuditEntry();
        entry.entityType = event.entityType();
        entry.entityId = event.entityId();
        entry.action = event.action();
        entry.reason = event.reason();
        entry.performedBy = event.performedBy();
        entry.performedAt = at;
        if (change != null) {
            entry.fieldName = change.field();
            entry.oldValue = change.oldValue();
            entry.newValue = change.newValue();
        }
        return entry;
    }

    public UUID getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public AuditAction getAction() { return action; }
    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getReason() { return reason; }
    public String getPerformedBy() { return performedBy; }
    public Instant getPerformedAt() { return performedAt; }
}
