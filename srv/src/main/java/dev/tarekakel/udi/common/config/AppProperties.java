package dev.tarekakel.udi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code app.*} configuration block; secrets arrive via .env locally and VCAP/env on BTP. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(boolean seedData, OpenAi openai) {

    public record OpenAi(String apiKey, String model) {
        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
