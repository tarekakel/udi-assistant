package dev.tarekakel.udi.device;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo data, created through the service so every record has a genuine audit trail.
 * Manufacturers are fictional. Enabled with {@code app.seed-data=true}.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
class DeviceSeeder implements ApplicationRunner {

    private final DeviceService devices;
    private final DeviceRepository repository;

    DeviceSeeder(DeviceService devices, DeviceRepository repository) {
        this.devices = devices;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        UUID registered = seed("04012345678901", "NeuroCath ICP Monitoring Catheter", "Fulda Medical Systems GmbH", RiskClass.III);
        UUID submitted = seed("04012345678918", "DermaSeal Wound Dressing 10x10", "Hersfeld Biomaterials AG", RiskClass.IIA);
        seed("04098765432101", "OrthoFix Titanium Bone Screw 4.5", "Kassel Implant Technologies", RiskClass.IIB);
        UUID withdrawn = seed("04055500011120", "ClearView Examination Gloves", "Werra Disposables GmbH", RiskClass.I);
        seed("04077788899931", "PulseTrack Wearable ECG Patch", "Fulda Medical Systems GmbH", RiskClass.IIA);

        transition(registered, RegistrationStatus.SUBMITTED, "Technical documentation complete");
        transition(registered, RegistrationStatus.REGISTERED, "EUDAMED registration confirmed");
        transition(submitted, RegistrationStatus.SUBMITTED, "Submitted for notified body review");
        transition(withdrawn, RegistrationStatus.SUBMITTED, "Submitted");
        transition(withdrawn, RegistrationStatus.REGISTERED, "Registered");
        transition(withdrawn, RegistrationStatus.WITHDRAWN, "Product line discontinued");
    }

    private UUID seed(String udiDi, String name, String manufacturer, RiskClass riskClass) {
        return devices.create(new CreateDeviceRequest(udiDi, name, manufacturer, riskClass)).getId();
    }

    private void transition(UUID id, RegistrationStatus status, String reason) {
        devices.changeStatus(id, new ChangeStatusRequest(status, reason));
    }
}
