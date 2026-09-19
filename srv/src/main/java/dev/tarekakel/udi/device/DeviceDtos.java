package dev.tarekakel.udi.device;

import dev.tarekakel.udi.common.validation.Gtin14;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
