package dev.tarekakel.udi.assistant;

/** One section of one regulation source file; the unit that is embedded, retrieved and cited. */
public record RegulationChunk(String id, String source, String section, String text) {
}
