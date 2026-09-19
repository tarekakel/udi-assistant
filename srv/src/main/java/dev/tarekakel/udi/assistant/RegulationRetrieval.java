package dev.tarekakel.udi.assistant;

import dev.tarekakel.udi.common.config.AppProperties;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Retrieval on its own, without generation: the passages an answer would rest on. The assistant uses it before
 * calling the model; the MCP tools expose it directly so an external agent gets evidence, not opinions.
 * Kept separate from {@link AssistantService} so tools never depend on the chat client (which itself collects
 * every tool in the context).
 */
@Service
public class RegulationRetrieval {

    private final RegulationIndex index;
    private final AppProperties.OpenAi openAi;

    RegulationRetrieval(RegulationIndex index, AppProperties properties) {
        this.index = index;
        this.openAi = properties.openai();
    }

    public List<Citation> retrieve(String question) {
        if (!openAi.configured()) {
            throw new AssistantNotConfiguredException();
        }
        return index.search(question);
    }
}
