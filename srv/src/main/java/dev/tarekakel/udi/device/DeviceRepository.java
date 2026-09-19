package dev.tarekakel.udi.device;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    boolean existsByUdiDi(String udiDi);

    Optional<Device> findByUdiDi(String udiDi);

    Page<Device> findByRegistrationStatus(RegistrationStatus status, Pageable pageable);
}
