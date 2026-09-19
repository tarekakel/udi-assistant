package dev.tarekakel.udi.device;

import dev.tarekakel.udi.common.validation.Gtin14;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

record CreateDeviceRequest(
        @NotBlank @Gtin14 String udiDi,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String manufacturer,
        @NotNull RiskClass riskClass) {
}

/** Every change to a record carries a reason (21 CFR Part 11 / Annex 11 expectation) and the version the editor saw. */
record UpdateDeviceRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String manufacturer,
        @NotNull RiskClass riskClass,
        @NotNull Long version,
        @NotBlank @Size(max = 500) String reason) {
}

record ChangeStatusRequest(
        @NotNull RegistrationStatus status,
        @NotBlank @Size(max = 500) String reason) {
}

record DeviceResponse(
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

    static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getUdiDi(), d.getName(), d.getManufacturer(), d.getRiskClass(),
                d.getRegistrationStatus(), d.getRegistrationStatus().allowedTransitions(), d.getVersion(),
                d.getCreatedAt(), d.getCreatedBy(), d.getUpdatedAt(), d.getUpdatedBy());
    }
}
