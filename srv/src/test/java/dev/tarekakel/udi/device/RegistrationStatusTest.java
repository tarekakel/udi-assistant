package dev.tarekakel.udi.device;

import static dev.tarekakel.udi.device.RegistrationStatus.DRAFT;
import static dev.tarekakel.udi.device.RegistrationStatus.REGISTERED;
import static dev.tarekakel.udi.device.RegistrationStatus.SUBMITTED;
import static dev.tarekakel.udi.device.RegistrationStatus.WITHDRAWN;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistrationStatusTest {

    @Test
    void followsTheLifecycle() {
        assertThat(DRAFT.canTransitionTo(SUBMITTED)).isTrue();
        assertThat(SUBMITTED.canTransitionTo(REGISTERED)).isTrue();
        assertThat(SUBMITTED.canTransitionTo(DRAFT)).isTrue();
        assertThat(REGISTERED.canTransitionTo(WITHDRAWN)).isTrue();
    }

    @Test
    void rejectsShortcutsAndLeavingTheTerminalState() {
        assertThat(DRAFT.canTransitionTo(REGISTERED)).isFalse();
        assertThat(REGISTERED.canTransitionTo(DRAFT)).isFalse();
        assertThat(WITHDRAWN.allowedTransitions()).isEmpty();
    }
}
