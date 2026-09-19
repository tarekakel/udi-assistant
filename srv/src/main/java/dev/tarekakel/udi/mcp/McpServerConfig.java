package dev.tarekakel.udi.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the {@link UdiTools} methods as MCP tools. Transport, protocol and capabilities are configured under
 * {@code spring.ai.mcp.server} in application.yaml (stateless HTTP at {@code /mcp}); on BTP the endpoint sits
 * behind the same JWT filter as the REST API and expects a token with the Viewer or Editor scope.
 */
@Configuration
class McpServerConfig {

    @Bean
    ToolCallbackProvider udiToolCallbacks(UdiTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
