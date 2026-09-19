package dev.tarekakel.udi.assistant;

/**
 * Prompt contract. The model may only use the supplied context and must cite it; anything outside the context is
 * declared "not covered". Templates use Spring AI's {placeholder} syntax, so they contain no other braces.
 */
final class AssistantPrompts {

    static final String SYSTEM = """
            You are a regulatory assistant for medical-device UDI master data in a GxP environment.
            Rules:
            - Answer ONLY from the passages in CONTEXT. Do not use outside knowledge.
            - After every statement, cite the passage it comes from using its label exactly as given, e.g. [udi-basics § UDI-DI versus UDI-PI].
            - If the context does not cover the question, reply exactly: "Not covered by the regulation excerpts available to me." and nothing else.
            - Be precise and concise. Never invent article numbers, deadlines or requirements.
            """;

    static final String ASK = """
            CONTEXT:
            {context}

            QUESTION:
            {question}
            """;

    static final String REVIEW = """
            Review the following device master-data record against the regulatory passages in CONTEXT.
            Report only findings that the passages support; cite the passage label in the "source" and "section" fields.
            Use severity CRITICAL for a likely non-compliance, WARN for something to verify, INFO for a helpful note.
            If the record raises no issue supported by the context, return an empty findings list.

            CONTEXT:
            {context}

            DEVICE RECORD:
            {device}
            """;

    private AssistantPrompts() {
    }

    static String renderContext(java.util.List<Citation> citations) {
        if (citations.isEmpty()) {
            return "(no relevant passages found)";
        }
        var out = new StringBuilder();
        for (Citation citation : citations) {
            out.append(citation.label()).append('\n').append(citation.excerpt()).append("\n\n");
        }
        return out.toString().strip();
    }
}
