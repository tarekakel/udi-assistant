package dev.tarekakel.udi.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.tarekakel.udi.assistant.Citation;
import dev.tarekakel.udi.assistant.RegulationIndex;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End to end over the wire: a real MCP client (the official SDK) talks to the running server over stateless HTTP.
 * The vector index is stubbed so the test never calls OpenAI; everything else is the production wiring.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"app.seed-data=true", "app.openai.api-key=test-key"})
class McpServerTest {

    private static final String SEEDED_UDI_DI = "04012345678901";   // NeuroCath ICP Monitoring Catheter, class III

    @MockitoBean
    private RegulationIndex index;

    @LocalServerPort
    private int port;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build();
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build();
        client.initialize();
        when(index.search(anyString())).thenReturn(List.of(
                new Citation("udi-basics", "UDI-DI versus UDI-PI", "The UDI-DI is static; the UDI-PI is dynamic.", 0.91)));
    }

    @AfterEach
    void disconnect() {
        client.closeGracefully();
    }

    @Test
    void exposesExactlyTheReadOnlyTools() {
        List<String> names = client.listTools().tools().stream().map(McpSchema.Tool::name).toList();

        assertThat(names).containsExactlyInAnyOrder("findDevice", "searchDevices", "getAuditTrail", "searchRegulation");
    }

    @Test
    void findDeviceReturnsMasterDataAndAllowedTransitions() {
        McpSchema.CallToolResult result = call("findDevice", Map.of("udiDi", SEEDED_UDI_DI));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("NeuroCath").contains("\"riskClass\":\"III\"").contains("WITHDRAWN");
    }

    @Test
    void unknownDeviceIsAToolErrorTheAgentCanRead() {
        McpSchema.CallToolResult result = call("findDevice", Map.of("udiDi", "04000000000006"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("No device with UDI-DI 04000000000006");
    }

    @Test
    void searchDevicesFiltersByTextAndStatus() {
        McpSchema.CallToolResult result = call("searchDevices",
                Map.of("text", "gmbh", "status", "SUBMITTED", "limit", 50));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("\"registrationStatus\":\"SUBMITTED\"").doesNotContain("\"registrationStatus\":\"REGISTERED\"");
    }

    @Test
    void auditTrailStartsWithTheRegistration() {
        McpSchema.CallToolResult result = call("getAuditTrail", Map.of("udiDi", SEEDED_UDI_DI));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("\"action\":\"CREATE\"").contains("Initial registration");
    }

    @Test
    void searchRegulationReturnsPassagesWithCitationsNotAnswers() {
        McpSchema.CallToolResult result = call("searchRegulation", Map.of("question", "What is a UDI-PI?"));

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("\"source\":\"udi-basics\"").contains("UDI-DI versus UDI-PI");
    }

    private McpSchema.CallToolResult call(String tool, Map<String, Object> arguments) {
        return client.callTool(McpSchema.CallToolRequest.builder(tool).arguments(arguments).build());
    }

    private static String text(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .reduce("", String::concat);
    }
}
