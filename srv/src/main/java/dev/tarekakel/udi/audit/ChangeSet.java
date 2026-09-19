package dev.tarekakel.udi.audit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The diff between two attribute snapshots of an aggregate. Computed in one place, reused by every audited entity. */
public record ChangeSet(List<FieldChange> changes) {

    public static ChangeSet none() {
        return new ChangeSet(List.of());
    }

    public static ChangeSet between(Map<String, ?> before, Map<String, ?> after) {
        var fields = new LinkedHashSet<String>(before.keySet());
        fields.addAll(after.keySet());
        var changes = new ArrayList<FieldChange>();
        for (String field : fields) {
            Object oldValue = before.get(field);
            Object newValue = after.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new FieldChange(field, render(oldValue), render(newValue)));
            }
        }
        return new ChangeSet(List.copyOf(changes));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    private static String render(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
