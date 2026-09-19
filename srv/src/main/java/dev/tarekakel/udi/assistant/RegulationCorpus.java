package dev.tarekakel.udi.assistant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * The assistant's knowledge, versioned with the application: Markdown files under {@code classpath:regulation/},
 * one file per source, one chunk per {@code ##} section. Nothing is fetched at runtime, so what the assistant can
 * cite is exactly what was reviewed and shipped.
 */
@Component
public class RegulationCorpus {

    private static final String LOCATION = "classpath:regulation/*.md";
    private static final String SECTION_MARKER = "## ";
    private static final String TITLE_MARKER = "# ";

    private final List<RegulationChunk> chunks;

    public RegulationCorpus() {
        this.chunks = load();
    }

    public List<RegulationChunk> chunks() {
        return chunks;
    }

    private static List<RegulationChunk> load() {
        try {
            var chunks = new ArrayList<RegulationChunk>();
            for (Resource file : new PathMatchingResourcePatternResolver().getResources(LOCATION)) {
                chunks.addAll(parse(sourceName(file), file.getContentAsString(StandardCharsets.UTF_8)));
            }
            return List.copyOf(chunks);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load regulation corpus from " + LOCATION, e);
        }
    }

    static List<RegulationChunk> parse(String source, String markdown) {
        var chunks = new ArrayList<RegulationChunk>();
        String section = null;
        var body = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            if (line.startsWith(SECTION_MARKER)) {
                addChunk(chunks, source, section, body);
                section = line.substring(SECTION_MARKER.length()).trim();
                body.setLength(0);
            } else if (!line.startsWith(TITLE_MARKER)) {
                body.append(line).append('\n');
            }
        }
        addChunk(chunks, source, section, body);
        return chunks;
    }

    private static void addChunk(List<RegulationChunk> chunks, String source, String section, StringBuilder body) {
        String text = body.toString().strip();
        if (section != null && !text.isEmpty()) {
            chunks.add(new RegulationChunk(source + "#" + slug(section), source, section, text));
        }
    }

    private static String sourceName(Resource file) {
        String name = file.getFilename() == null ? "unknown" : file.getFilename();
        return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
    }

    private static String slug(String heading) {
        return heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
