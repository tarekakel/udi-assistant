package dev.tarekakel.udi.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChangeSetTest {

    @Test
    void reportsOnlyFieldsThatDiffer() {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("name", "A");
        before.put("riskClass", "I");
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.put("name", "B");

        ChangeSet changes = ChangeSet.between(before, after);

        assertThat(changes.changes()).containsExactly(new FieldChange("name", "A", "B"));
    }

    @Test
    void isEmptyWhenNothingChanged() {
        Map<String, Object> snapshot = Map.of("name", "A");

        assertThat(ChangeSet.between(snapshot, snapshot).isEmpty()).isTrue();
    }

    @Test
    void rendersNullsAndNewFields() {
        ChangeSet changes = ChangeSet.between(Map.of(), Map.of("name", "A"));

        assertThat(changes.changes()).containsExactly(new FieldChange("name", null, "A"));
    }
}
