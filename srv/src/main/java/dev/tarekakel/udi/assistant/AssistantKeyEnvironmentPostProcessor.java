package dev.tarekakel.udi.assistant;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * The assistant is optional, but Spring AI's OpenAI auto-configuration refuses to start with an empty key.
 * When no key is configured this installs a placeholder for Spring AI only; {@code app.openai.api-key} stays
 * blank, so the assistant reports "not configured" instead of calling OpenAI with a bogus key.
 * Registered in META-INF/spring.factories.
 */
public class AssistantKeyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String APP_KEY = "app.openai.api-key";
    static final String SPRING_AI_KEY = "spring.ai.openai.api-key";
    static final String PLACEHOLDER = "not-configured";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String key = environment.getProperty(APP_KEY);
        if (key == null || key.isBlank()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("assistant-key-fallback", Map.of(SPRING_AI_KEY, PLACEHOLDER)));
        }
    }
}
