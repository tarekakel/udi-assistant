package dev.tarekakel.udi.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegulationCorpusTest {

    @Test
    void splitsAFileIntoOneChunkPerSection() {
        String markdown = """
                # Title line is dropped

                ## First section
                Body of the first section.

                ## Second section
                Body of the
                second section.
                """;

        List<RegulationChunk> chunks = RegulationCorpus.parse("sample", markdown);

        assertThat(chunks).extracting(RegulationChunk::id).containsExactly("sample#first-section", "sample#second-section");
        assertThat(chunks.get(1).text()).isEqualTo("Body of the\nsecond section.");
        assertThat(chunks.get(0).section()).isEqualTo("First section");
    }

    @Test
    void shippedCorpusIsNonEmptyWithUniqueIdsAndNoBlankChunks() {
        List<RegulationChunk> chunks = new RegulationCorpus().chunks();

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(20);
        assertThat(chunks).extracting(RegulationChunk::id).doesNotHaveDuplicates();
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.section()).isNotBlank();
            assertThat(chunk.text()).hasSizeBetween(120, 1500);
        });
        assertThat(chunks).extracting(RegulationChunk::source)
                .contains("udi-basics", "eudamed", "labelling", "risk-classes", "udi-changes", "audit-trail", "gudid");
    }
}
