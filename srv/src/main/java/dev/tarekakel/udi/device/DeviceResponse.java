package dev.tarekakel.udi.device;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** The one representation of a device that leaves the module: the REST API and the MCP tools both return it. */
public record DeviceResponse(
        UUID id,
        String udiDi,
        String name,
        String manufacturer,
        RiskClass riskClass,
        RegistrationStatus registrationStatus,
        Set<RegistrationStatus> allowedTransitions,
        Long version,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy) {

    public static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getUdiDi(), d.getName(), d.getManufacturer(), d.getRiskClass(),
                d.getRegistrationStatus(), d.getRegistrationStatus().allowedTransitions(), d.getVersion(),
                d.getCreatedAt(), d.getCreatedBy(), d.getUpdatedAt(), d.getUpdatedBy());
    }
}
