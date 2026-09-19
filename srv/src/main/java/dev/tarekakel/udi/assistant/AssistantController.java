package dev.tarekakel.udi.assistant;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class AssistantController {

    private final AssistantService assistant;
    private final RegulationCorpus corpus;

    AssistantController(AssistantService assistant, RegulationCorpus corpus) {
        this.assistant = assistant;
        this.corpus = corpus;
    }

    /** Viewer scope: answering a question changes nothing. */
    @PostMapping("/assistant/ask")
    Answer ask(@Valid @RequestBody AskRequest request) {
        return assistant.ask(request.question());
    }

    /** What the assistant knows, so a reviewer can see the boundaries of its answers. */
    @GetMapping("/assistant/sources")
    List<SourceSummary> sources() {
        var bySource = new LinkedHashMap<String, List<String>>();
        corpus.chunks().forEach(chunk -> bySource.computeIfAbsent(chunk.source(), s -> new java.util.ArrayList<>()).add(chunk.section()));
        return bySource.entrySet().stream().map(e -> new SourceSummary(e.getKey(), List.copyOf(e.getValue()))).toList();
    }

    /** Editor scope: a review is an editorial act on the record even though it does not change it. */
    @PostMapping("/devices/{id}/review")
    Review review(@PathVariable UUID id) {
        return assistant.review(id);
    }
}
