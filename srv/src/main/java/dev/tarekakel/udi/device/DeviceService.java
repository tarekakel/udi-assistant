package dev.tarekakel.udi.device;

import dev.tarekakel.udi.audit.AuditAction;
import dev.tarekakel.udi.audit.ChangeSet;
import dev.tarekakel.udi.audit.EntityChangedEvent;
import dev.tarekakel.udi.common.security.CurrentUserProvider;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service: enforces invariants, then announces what changed. It never writes the audit trail itself. */
@Service
@Transactional
public class DeviceService {

    private static final String INITIAL_REASON = "Initial registration";

    private final DeviceRepository devices;
    private final ApplicationEventPublisher events;
    private final CurrentUserProvider users;

    DeviceService(DeviceRepository devices, ApplicationEventPublisher events, CurrentUserProvider users) {
        this.devices = devices;
        this.events = events;
        this.users = users;
    }

    public Device create(CreateDeviceRequest request) {
        if (devices.existsByUdiDi(request.udiDi())) {
            throw new DuplicateUdiDiException(request.udiDi());
        }
        Device device = devices.save(Device.register(
                request.udiDi(), request.name(), request.manufacturer(), request.riskClass()));
        publish(device, AuditAction.CREATE, ChangeSet.none(), INITIAL_REASON);
        return device;
    }

    public Device update(UUID id, UpdateDeviceRequest request) {
        Device device = require(id);
        assertVersion(device, request.version());
        Map<String, Object> before = device.auditSnapshot();
        device.updateDetails(request.name(), request.manufacturer(), request.riskClass());
        ChangeSet changes = ChangeSet.between(before, device.auditSnapshot());
        if (!changes.isEmpty()) {
            publish(device, AuditAction.UPDATE, changes, request.reason());
            devices.flush();
        }
        return device;
    }

    public Device changeStatus(UUID id, ChangeStatusRequest request) {
        Device device = require(id);
        Map<String, Object> before = device.auditSnapshot();
        device.transitionTo(request.status());
        publish(device, AuditAction.STATUS_CHANGE, ChangeSet.between(before, device.auditSnapshot()), request.reason());
        devices.flush();
        return device;
    }

    @Transactional(readOnly = true)
    public Device get(UUID id) {
        return require(id);
    }

    @Transactional(readOnly = true)
    public Device getByUdiDi(String udiDi) {
        return devices.findByUdiDi(udiDi).orElseThrow(() -> new DeviceNotFoundException(udiDi));
    }

    /** Free-text search over UDI-DI, name and manufacturer, optionally narrowed to one status. */
    @Transactional(readOnly = true)
    public Page<Device> search(String text, RegistrationStatus status, Pageable pageable) {
        return devices.findAll(DeviceSpecifications.matching(text, status), pageable);
    }

    private Device require(UUID id) {
        return devices.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    private static void assertVersion(Device device, long expected) {
        if (device.getVersion() != expected) {
            throw new StaleDeviceException(device.getUdiDi(), expected, device.getVersion());
        }
    }

    private void publish(Device device, AuditAction action, ChangeSet changes, String reason) {
        events.publishEvent(new EntityChangedEvent(
                Device.ENTITY_TYPE, device.getId().toString(), action, changes, reason, users.currentUser()));
    }
}
