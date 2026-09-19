package dev.tarekakel.udi.audit;

/** One attribute transition, already rendered to text so the trail is readable without the domain model. */
public record FieldChange(String field, String oldValue, String newValue) {
}
