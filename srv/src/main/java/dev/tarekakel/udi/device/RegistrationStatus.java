package dev.tarekakel.udi.device;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of a UDI-DI registration. Each state owns its legal successors, so the rule lives in one place
 * and the API can tell clients what is allowed next. Records are never deleted: WITHDRAWN is the terminal state.
 */
public enum RegistrationStatus {
    DRAFT, SUBMITTED, REGISTERED, WITHDRAWN;

    private static final Map<RegistrationStatus, Set<RegistrationStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(SUBMITTED),
            SUBMITTED, EnumSet.of(REGISTERED, DRAFT),
            REGISTERED, EnumSet.of(WITHDRAWN),
            WITHDRAWN, EnumSet.noneOf(RegistrationStatus.class));

    public boolean canTransitionTo(RegistrationStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** Declaration order is preserved so clients (and tests) see a stable sequence. */
    public Set<RegistrationStatus> allowedTransitions() {
        return Collections.unmodifiableSet(EnumSet.copyOf(TRANSITIONS.get(this)));
    }
}
