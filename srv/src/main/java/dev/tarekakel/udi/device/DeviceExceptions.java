package dev.tarekakel.udi.device;

import dev.tarekakel.udi.common.api.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

class DeviceNotFoundException extends DomainException {
    DeviceNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Device not found", "No device with id " + id);
    }
}

class DuplicateUdiDiException extends DomainException {
    DuplicateUdiDiException(String udiDi) {
        super(HttpStatus.CONFLICT, "Duplicate UDI-DI", "A device with UDI-DI " + udiDi + " already exists");
    }
}

class InvalidStatusTransitionException extends DomainException {
    InvalidStatusTransitionException(String udiDi, RegistrationStatus from, RegistrationStatus to) {
        super(HttpStatus.CONFLICT, "Invalid status transition",
                "Device " + udiDi + " cannot move from " + from + " to " + to + "; allowed: " + from.allowedTransitions());
    }
}

class StaleDeviceException extends DomainException {
    StaleDeviceException(String udiDi, long expected, long actual) {
        super(HttpStatus.CONFLICT, "Stale version",
                "Device " + udiDi + " was modified by someone else (you sent version " + expected + ", current is " + actual + ")");
    }
}
