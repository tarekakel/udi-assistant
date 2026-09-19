package dev.tarekakel.udi.assistant;

import dev.tarekakel.udi.common.config.AppProperties;
import dev.tarekakel.udi.device.Device;
import dev.tarekakel.udi.device.DeviceService;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Retrieval-augmented answers over the regulation corpus. Retrieval decides what the model may see; the prompt
 * forbids anything else; citations are attached from retrieval metadata, so they cannot be hallucinated.
 */
@Service
public class AssistantService {

    private final RegulationRetrieval retrieval;
    private final ChatClient chat;
    private final DeviceService devices;
    private final AppProperties.OpenAi openAi;

    AssistantService(RegulationRetrieval retrieval, ChatClient.Builder chatBuilder, DeviceService devices, AppProperties properties) {
        this.retrieval = retrieval;
        this.chat = chatBuilder.defaultSystem(AssistantPrompts.SYSTEM).build();
        this.devices = devices;
        this.openAi = properties.openai();
    }

    public Answer ask(String question) {
        List<Citation> citations = retrieval.retrieve(question);
        String answer = chat.prompt()
                .user(user -> user.text(AssistantPrompts.ASK)
                        .param("context", AssistantPrompts.renderContext(citations))
                        .param("question", question))
                .call()
                .content();
        return new Answer(answer, citations, openAi.model());
    }

    public Review review(UUID deviceId) {
        Device device = devices.get(deviceId);
        List<Citation> citations = retrieval.retrieve(reviewQuery(device));
        ReviewFindings findings = chat.prompt()
                .user(user -> user.text(AssistantPrompts.REVIEW)
                        .param("context", AssistantPrompts.renderContext(citations))
                        .param("device", describe(device)))
                .call()
                .entity(ReviewFindings.class);
        List<Finding> list = findings == null || findings.findings() == null ? List.of() : findings.findings();
        return new Review(device.getId(), device.getUdiDi(), list, citations, openAi.model());
    }

    /** What to retrieve for a review: labelling, registration and classification passages for this kind of device. */
    private static String reviewQuery(Device device) {
        return "UDI-DI registration, labelling and change requirements for a class " + device.getRiskClass()
                + " medical device with registration status " + device.getRegistrationStatus();
    }

    private static String describe(Device device) {
        return "UDI-DI: " + device.getUdiDi()
                + "\nName: " + device.getName()
                + "\nManufacturer: " + device.getManufacturer()
                + "\nRisk class: " + device.getRiskClass()
                + "\nRegistration status: " + device.getRegistrationStatus()
                + "\nRecord version: " + device.getVersion();
    }
}
