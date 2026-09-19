package dev.tarekakel.udi.device;

import dev.tarekakel.udi.audit.AuditEntryResponse;
import dev.tarekakel.udi.audit.AuditTrailQuery;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** No DELETE by design: regulated records are withdrawn through the status lifecycle, never removed. */
@RestController
@RequestMapping("/api/devices")
class DeviceController {

    private final DeviceService devices;
    private final AuditTrailQuery auditTrail;

    DeviceController(DeviceService devices, AuditTrailQuery auditTrail) {
        this.devices = devices;
        this.auditTrail = auditTrail;
    }

    @GetMapping
    PagedModel<DeviceResponse> list(@RequestParam(required = false) RegistrationStatus status,
                                    @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return new PagedModel<>(devices.list(status, pageable).map(DeviceResponse::from));
    }

    @GetMapping("/{id}")
    DeviceResponse get(@PathVariable UUID id) {
        return DeviceResponse.from(devices.get(id));
    }

    @PostMapping
    ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request) {
        Device device = devices.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(device.getId());
        return ResponseEntity.created(location).body(DeviceResponse.from(device));
    }

    @PutMapping("/{id}")
    DeviceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDeviceRequest request) {
        return DeviceResponse.from(devices.update(id, request));
    }

    @PostMapping("/{id}/status")
    DeviceResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return DeviceResponse.from(devices.changeStatus(id, request));
    }

    @GetMapping("/{id}/audit-trail")
    List<AuditEntryResponse> auditTrail(@PathVariable UUID id) {
        devices.get(id);
        return auditTrail.forEntity(Device.ENTITY_TYPE, id.toString());
    }
}
