package dev.tarekakel.udi.mcp;

import dev.tarekakel.udi.assistant.Citation;
import dev.tarekakel.udi.assistant.RegulationRetrieval;
import dev.tarekakel.udi.audit.AuditEntryResponse;
import dev.tarekakel.udi.audit.AuditTrailQuery;
import dev.tarekakel.udi.device.Device;
import dev.tarekakel.udi.device.DeviceResponse;
import dev.tarekakel.udi.device.DeviceService;
import dev.tarekakel.udi.device.RegistrationStatus;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * The tools an AI agent may call over MCP. Read-only by design: agents look things up and cite; people change
 * master data through the application and must give a reason (SPEC M2). Every tool delegates to the same
 * application services the REST API uses, so validation, error messages and scope checks are identical for both.
 */
@Component
class UdiTools {

    private static final int DEFAULT_RESULTS = 10;
    private static final int MAX_RESULTS = 50;

    private final DeviceService devices;
    private final AuditTrailQuery auditTrail;
    private final RegulationRetrieval regulation;

    UdiTools(DeviceService devices, AuditTrailQuery auditTrail, RegulationRetrieval regulation) {
        this.devices = devices;
        this.auditTrail = auditTrail;
        this.regulation = regulation;
    }

    @Tool(name = "findDevice", description = """
            Look up one medical device by its UDI-DI (GS1 GTIN-14, 14 digits). Returns the master data, the \
            registration status and the status transitions allowed next.""")
    public DeviceResponse findDevice(@ToolParam(description = "The 14-digit UDI-DI, e.g. 04012345678901") String udiDi) {
        return DeviceResponse.from(devices.getByUdiDi(udiDi));
    }

    @Tool(name = "searchDevices", description = """
            Search devices by free text over UDI-DI, name and manufacturer, optionally limited to one registration \
            status. Results are sorted by name.""")
    public List<DeviceResponse> searchDevices(
            @ToolParam(required = false, description = "Text to match, case-insensitive; omit for all devices") String text,
            @ToolParam(required = false, description = "DRAFT, SUBMITTED, REGISTERED or WITHDRAWN") RegistrationStatus status,
            @ToolParam(required = false, description = "Maximum number of results, 1-50, default 10") Integer limit) {
        int size = limit == null ? DEFAULT_RESULTS : Math.max(1, Math.min(MAX_RESULTS, limit));
        return devices.search(text, status, PageRequest.of(0, size, Sort.by("name")))
                .map(DeviceResponse::from)
                .getContent();
    }

    @Tool(name = "getAuditTrail", description = """
            The complete audit trail of one device: who changed which field from what to what, when, and the \
            reason given. Oldest entry first.""")
    public List<AuditEntryResponse> getAuditTrail(@ToolParam(description = "The 14-digit UDI-DI") String udiDi) {
        Device device = devices.getByUdiDi(udiDi);
        return auditTrail.forEntity(Device.ENTITY_TYPE, device.getId().toString());
    }

    @Tool(name = "searchRegulation", description = """
            Retrieve the passages of the UDI / EU MDR regulation excerpts most relevant to a question. Returns \
            passages with source and section for citation; it does not generate an answer.""")
    public List<Citation> searchRegulation(@ToolParam(description = "The question or topic") String question) {
        return regulation.retrieve(question);
    }
}
