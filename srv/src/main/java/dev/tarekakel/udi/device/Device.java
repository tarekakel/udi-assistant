package dev.tarekakel.udi.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A medical device identified by its UDI-DI (GTIN-14). Aggregate root; all state changes go through its methods. */
@Entity
@Table(name = "device")
@EntityListeners(AuditingEntityListener.class)
public class Device {

    public static final String ENTITY_TYPE = "Device";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "udi_di", nullable = false, updatable = false, unique = true, length = 14)
    private String udiDi;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_class", nullable = false, length = 10)
    private RiskClass riskClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus;

    /** Optimistic locking: two editors cannot silently overwrite each other. */
    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    protected Device() {
    }

    public static Device register(String udiDi, String name, String manufacturer, RiskClass riskClass) {
        var device = new Device();
        device.udiDi = udiDi;
        device.name = name;
        device.manufacturer = manufacturer;
        device.riskClass = riskClass;
        device.registrationStatus = RegistrationStatus.DRAFT;
        return device;
    }

    public void updateDetails(String name, String manufacturer, RiskClass riskClass) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.riskClass = riskClass;
    }

    public void transitionTo(RegistrationStatus target) {
        if (!registrationStatus.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(udiDi, registrationStatus, target);
        }
        this.registrationStatus = target;
    }

    /** The attributes the audit trail tracks, in display order. */
    public Map<String, Object> auditSnapshot() {
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("name", name);
        snapshot.put("manufacturer", manufacturer);
        snapshot.put("riskClass", riskClass);
        snapshot.put("registrationStatus", registrationStatus);
        return snapshot;
    }

    public UUID getId() { return id; }
    public String getUdiDi() { return udiDi; }
    public String getName() { return name; }
    public String getManufacturer() { return manufacturer; }
    public RiskClass getRiskClass() { return riskClass; }
    public RegistrationStatus getRegistrationStatus() { return registrationStatus; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
