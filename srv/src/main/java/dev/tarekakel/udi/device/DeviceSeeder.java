package dev.tarekakel.udi.device;

import static dev.tarekakel.udi.device.RegistrationStatus.DRAFT;
import static dev.tarekakel.udi.device.RegistrationStatus.REGISTERED;
import static dev.tarekakel.udi.device.RegistrationStatus.SUBMITTED;
import static dev.tarekakel.udi.device.RegistrationStatus.WITHDRAWN;
import static dev.tarekakel.udi.device.RiskClass.I;
import static dev.tarekakel.udi.device.RiskClass.IIA;
import static dev.tarekakel.udi.device.RiskClass.IIB;
import static dev.tarekakel.udi.device.RiskClass.III;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo data, created through the service so every record has a genuine audit trail: each device is registered in
 * DRAFT and then walked through the lifecycle to its target status; a few receive detail changes with a reason.
 * Manufacturers are fictional. Enabled with {@code app.seed-data=true}.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
class DeviceSeeder implements ApplicationRunner {

    private record Seed(String udiDi, String name, String manufacturer, RiskClass riskClass, RegistrationStatus target) {
    }

    private static final String FULDA = "Fulda Medical Systems GmbH";
    private static final String HERSFELD = "Hersfeld Biomaterials AG";
    private static final String KASSEL = "Kassel Implant Technologies";
    private static final String WERRA = "Werra Disposables GmbH";
    private static final String RHOEN = "Rhoen Diagnostics GmbH";
    private static final String VOGELSBERG = "Vogelsberg Optics AG";
    private static final String EDER = "Eder Surgical Instruments GmbH";
    private static final String LAHN = "Lahn Respiratory Systems GmbH";

    private static final List<Seed> SEEDS = List.of(
            new Seed("04012345678901", "NeuroCath ICP Monitoring Catheter", FULDA, III, REGISTERED),
            new Seed("04012345678918", "DermaSeal Wound Dressing 10x10", HERSFELD, IIA, SUBMITTED),
            new Seed("04098765432101", "OrthoFix Titanium Bone Screw 4.5", KASSEL, IIB, DRAFT),
            new Seed("04055500011120", "ClearView Examination Gloves", WERRA, I, WITHDRAWN),
            new Seed("04077788899931", "PulseTrack Wearable ECG Patch", FULDA, IIA, DRAFT),
            new Seed("04011002003001", "VitaFlow Infusion Pump IP-200", LAHN, IIB, REGISTERED),
            new Seed("04011002003018", "AeroMist Home Nebuliser", LAHN, IIA, REGISTERED),
            new Seed("04011002003025", "SteriPack Surgical Drape Set", WERRA, I, REGISTERED),
            new Seed("04011002003032", "OptiLens Monofocal Intraocular Lens", VOGELSBERG, IIB, SUBMITTED),
            new Seed("04011002003049", "CardioMesh Vascular Stent 3.0", FULDA, III, SUBMITTED),
            new Seed("04022003004003", "FlexiCast Orthopaedic Cast Tape", KASSEL, I, DRAFT),
            new Seed("04022003004010", "ThermoScan Tympanic Thermometer", RHOEN, IIA, REGISTERED),
            new Seed("04022003004027", "GlucoSense CGM Sensor 14d", RHOEN, IIB, REGISTERED),
            new Seed("04022003004034", "HipCore Acetabular Cup System", KASSEL, III, DRAFT),
            new Seed("04022003004041", "LapraGrip Reusable Forceps 5 mm", EDER, I, REGISTERED),
            new Seed("04033004005005", "SonoWave Portable Ultrasound", VOGELSBERG, IIA, SUBMITTED),
            new Seed("04033004005012", "NeoVent Neonatal Ventilator", LAHN, IIB, DRAFT),
            new Seed("04033004005029", "HydroGel Burn Dressing 20x20", HERSFELD, IIB, REGISTERED),
            new Seed("04033004005036", "SpineLock Pedicle Screw System", KASSEL, IIB, WITHDRAWN),
            new Seed("04033004005043", "TeleCare Remote Monitoring App", FULDA, IIA, SUBMITTED));

    private static final Map<RegistrationStatus, List<RegistrationStatus>> PATH = Map.of(
            DRAFT, List.of(),
            SUBMITTED, List.of(SUBMITTED),
            REGISTERED, List.of(SUBMITTED, REGISTERED),
            WITHDRAWN, List.of(SUBMITTED, REGISTERED, WITHDRAWN));

    private static final Map<RegistrationStatus, String> REASON = Map.of(
            SUBMITTED, "Technical documentation complete; submitted for review",
            REGISTERED, "EUDAMED registration confirmed",
            WITHDRAWN, "Product line discontinued");

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
        for (Seed seed : SEEDS) {
            UUID id = devices.create(new CreateDeviceRequest(seed.udiDi(), seed.name(), seed.manufacturer(), seed.riskClass())).getId();
            PATH.get(seed.target()).forEach(status -> transition(id, status, REASON.get(status)));
        }
        // A rejected submission and two detail corrections, so the trail shows more than the happy path.
        UUID lens = idOf("04011002003032");
        transition(lens, DRAFT, "Notified body: technical file incomplete (clinical evaluation missing)");
        transition(lens, SUBMITTED, "Clinical evaluation report added; resubmitted");
        rename(idOf("04022003004010"), "ThermoScan Tympanic Thermometer", "Rhoen Diagnostics GmbH & Co. KG", IIA,
                "Legal entity change of the manufacturer");
        rename(idOf("04098765432101"), "OrthoFix Titanium Bone Screw 4.5 mm", KASSEL, IIB,
                "Label correction: unit added to the trade name");
    }

    private void transition(UUID id, RegistrationStatus status, String reason) {
        devices.changeStatus(id, new ChangeStatusRequest(status, reason));
    }

    private void rename(UUID id, String name, String manufacturer, RiskClass riskClass, String reason) {
        long version = devices.get(id).getVersion();
        devices.update(id, new UpdateDeviceRequest(name, manufacturer, riskClass, version, reason));
    }

    private UUID idOf(String udiDi) {
        return repository.findAll().stream().filter(d -> d.getUdiDi().equals(udiDi)).findFirst().orElseThrow().getId();
    }
}
