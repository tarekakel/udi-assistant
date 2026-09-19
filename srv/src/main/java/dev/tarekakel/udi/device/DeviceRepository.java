package dev.tarekakel.udi.device;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeviceRepository extends JpaRepository<Device, UUID> {

    boolean existsByUdiDi(String udiDi);

    Page<Device> findByRegistrationStatus(RegistrationStatus status, Pageable pageable);
}
