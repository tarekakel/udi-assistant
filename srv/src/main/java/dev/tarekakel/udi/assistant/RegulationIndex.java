package dev.tarekakel.udi.assistant;

import java.util.List;

/**
 * Retrieval side of the assistant. The in-memory vector implementation serves the demo; a HANA Cloud Vector Engine
 * or pgvector implementation would replace it without touching the service.
 */
public interface RegulationIndex {

    List<Citation> search(String query);
}
