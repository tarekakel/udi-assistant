package dev.tarekakel.udi.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

record AskRequest(@NotBlank @Size(max = 1000) String question) {
}

record Answer(String answer, List<Citation> citations, String model) {
}

enum Severity {
    INFO, WARN, CRITICAL
}

/** One review finding; {@code source}/{@code section} must name a passage from the supplied context. */
record Finding(Severity severity, String rule, String message, String source, String section) {
}

/** Shape the model is asked to return for a review (structured output). */
record ReviewFindings(List<Finding> findings) {
}

record Review(UUID deviceId, String udiDi, List<Finding> findings, List<Citation> citations, String model) {
}

record SourceSummary(String source, List<String> sections) {
}
