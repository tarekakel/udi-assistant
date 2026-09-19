package dev.tarekakel.udi.audit;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer side of the audit trail. Runs before the publishing transaction commits, so a business change
 * and its trail are written atomically: neither can exist without the other.
 */
@Component
class AuditTrailRecorder {

    private final AuditEntryRepository entries;
    private final Clock clock;

    AuditTrailRecorder(AuditEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(EntityChangedEvent event) {
        Instant now = clock.instant();
        if (event.changes().isEmpty()) {
            entries.save(AuditEntry.of(event, null, now));
            return;
        }
        event.changes().changes().forEach(change -> entries.save(AuditEntry.of(event, change, now)));
    }
}
