package dev.tarekakel.udi.assistant;

/** A retrieved passage the answer may rest on. Source and section come from corpus metadata, never from the model. */
public record Citation(String source, String section, String excerpt, double score) {

    public String label() {
        return "[" + source + " § " + section + "]";
    }
}
