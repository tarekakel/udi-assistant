package dev.tarekakel.udi.device;

import org.springframework.data.jpa.domain.Specification;

/** Composable query predicates for devices; the service combines them, callers never see JPA. */
final class DeviceSpecifications {

    private DeviceSpecifications() {
    }

    static Specification<Device> matching(String text, RegistrationStatus status) {
        return Specification.allOf(containsText(text), hasStatus(status));
    }

    /** Case-insensitive match on UDI-DI, name or manufacturer; blank text matches everything. */
    static Specification<Device> containsText(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("udiDi"), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("manufacturer")), pattern));
    }

    static Specification<Device> hasStatus(RegistrationStatus status) {
        return status == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("registrationStatus"), status);
    }
}
