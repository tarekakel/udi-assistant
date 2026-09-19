package dev.tarekakel.udi.assistant;

import dev.tarekakel.udi.common.config.AppProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Embeds the corpus once and answers similarity queries from memory. When an API key is configured the index is
 * warmed up in the background after startup; a failure there (wrong key, no network) is logged and retried on the
 * first real query, so the assistant can never prevent the master-data service from starting.
 */
@Component
class VectorRegulationIndex implements RegulationIndex, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorRegulationIndex.class);
    static final String SOURCE = "source";
    static final String SECTION = "section";

    private final RegulationCorpus corpus;
    private final VectorStore store;
    private final AppProperties properties;
    private volatile boolean indexed;

    VectorRegulationIndex(RegulationCorpus corpus, EmbeddingModel embeddingModel, AppProperties properties) {
        this.corpus = corpus;
        this.store = SimpleVectorStore.builder(embeddingModel).build();
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.openai().configured()) {
            log.info("Regulation assistant not configured (no OpenAI API key); {} chunks left unindexed", corpus.chunks().size());
            return;
        }
        CompletableFuture.runAsync(this::ensureIndexed).exceptionally(failure -> {
            log.warn("Regulation index warm-up failed; will retry on first query: {}", rootMessage(failure));
            return null;
        });
    }

    @Override
    public List<Citation> search(String query) {
        ensureIndexed();
        var request = SearchRequest.builder()
                .query(query)
                .topK(properties.assistant().topK())
                .similarityThreshold(properties.assistant().similarityThreshold())
                .build();
        return store.similaritySearch(request).stream().map(VectorRegulationIndex::toCitation).toList();
    }

    private synchronized void ensureIndexed() {
        if (indexed) {
            return;
        }
        long started = System.currentTimeMillis();
        store.add(corpus.chunks().stream().map(VectorRegulationIndex::toDocument).toList());
        indexed = true;
        log.info("Indexed {} regulation chunks in {} ms", corpus.chunks().size(), System.currentTimeMillis() - started);
    }

    private static Document toDocument(RegulationChunk chunk) {
        return new Document(chunk.id(), chunk.text(), Map.of(SOURCE, chunk.source(), SECTION, chunk.section()));
    }

    private static Citation toCitation(Document document) {
        return new Citation(
                String.valueOf(document.getMetadata().get(SOURCE)),
                String.valueOf(document.getMetadata().get(SECTION)),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore());
    }

    private static String rootMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
