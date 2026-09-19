package dev.tarekakel.udi.assistant;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Without an API key the application must still start and serve everything except the assistant. */
@SpringBootTest(properties = {"app.openai.api-key=", "OPENAI_API_KEY=", "app.seed-data=false"})
class AssistantNotConfiguredTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void assistantReports503AndTheRestOfTheApiWorks() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(post("/api/assistant/ask").contentType(APPLICATION_JSON).content("{\"question\":\"anything\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Assistant not configured"));

        mvc.perform(get("/api/devices")).andExpect(status().isOk());
        mvc.perform(get("/api/assistant/sources")).andExpect(status().isOk());
    }
}
