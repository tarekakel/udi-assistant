package dev.tarekakel.udi.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The model and the vector index are stubbed: these tests pin down the contract around them (what the model is
 * shown, how citations travel, how structured output is parsed) and never call OpenAI.
 */
@SpringBootTest(properties = {"app.openai.api-key=test-key", "app.seed-data=true"})
class AssistantApiTest {

    @MockitoBean
    private RegulationIndex index;

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(index.search(anyString())).thenReturn(List.of(
                new Citation("udi-basics", "UDI-DI versus UDI-PI", "The UDI-DI is static; the UDI-PI is dynamic.", 0.91)));
    }

    @Test
    void answersFromRetrievedContextAndReturnsCitations() throws Exception {
        modelReplies("The UDI-DI identifies the model; the UDI-PI identifies the production unit [udi-basics § UDI-DI versus UDI-PI].");

        mvc.perform(post("/api/assistant/ask").contentType(APPLICATION_JSON)
                        .content("{\"question\":\"What is the difference between UDI-DI and UDI-PI?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("UDI-PI")))
                .andExpect(jsonPath("$.citations[0].source").value("udi-basics"))
                .andExpect(jsonPath("$.citations[0].section").value("UDI-DI versus UDI-PI"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        String sent = prompt.getValue().getContents();
        assertThat(sent).contains("Answer ONLY from the passages in CONTEXT");
        assertThat(sent).contains("[udi-basics § UDI-DI versus UDI-PI]");
        assertThat(sent).contains("What is the difference between UDI-DI and UDI-PI?");
    }

    @Test
    void rejectsBlankQuestions() throws Exception {
        mvc.perform(post("/api/assistant/ask").contentType(APPLICATION_JSON).content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("question"));
    }

    @Test
    void listsWhatTheAssistantKnows() throws Exception {
        mvc.perform(get("/api/assistant/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.source=='udi-basics')].sections[0]").value("What a UDI is"));
    }

    @Test
    void reviewParsesStructuredFindingsForASeededDevice() throws Exception {
        modelReplies("""
                {"findings":[{"severity":"WARN","rule":"Direct marking","message":"Reusable class IIb device: verify direct marking of the UDI.","source":"labelling","section":"Direct marking of reusable devices"}]}
                """);
        String id = mvc.perform(get("/api/devices").param("status", "DRAFT"))
                .andReturn().getResponse().getContentAsString().replaceAll("(?s).*?\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/devices/{id}/review", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(id))
                .andExpect(jsonPath("$.findings[0].severity").value("WARN"))
                .andExpect(jsonPath("$.findings[0].section").value("Direct marking of reusable devices"))
                .andExpect(jsonPath("$.citations[0].source").value("udi-basics"));
    }

    private void modelReplies(String text) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }
}
