package dev.tarekakel.udi.assistant;

import dev.tarekakel.udi.common.api.DomainException;
import org.springframework.http.HttpStatus;

/** The assistant is an optional capability: without an API key it reports 503 instead of breaking the application. */
class AssistantNotConfiguredException extends DomainException {

    AssistantNotConfiguredException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Assistant not configured",
                "No OpenAI API key is configured; set OPENAI_API_KEY (locally) or bind the 'openai' user-provided service (BTP)");
    }
}
