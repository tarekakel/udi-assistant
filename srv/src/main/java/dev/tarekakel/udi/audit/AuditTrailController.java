package dev.tarekakel.udi.audit;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuditTrailController {

    private final AuditTrailQuery auditTrail;

    AuditTrailController(AuditTrailQuery auditTrail) {
        this.auditTrail = auditTrail;
    }

    @GetMapping("/api/audit-trail")
    List<AuditEntryResponse> recent() {
        return auditTrail.recent();
    }
}
