package com.tradebridge.notification.template;

import java.util.Objects;

/**
 * A single variable extracted from a template string.
 *
 * <p>Extraction is regex-based over `${var}` occurrences. Only names
 * matching {@code [A-Za-z0-9_.]+} are accepted — anything else is
 * ignored, and the raw token is treated as literal text after escaping.
 */
public record TemplateVariable(String name, int startIndex, int endIndex) {

    public TemplateVariable {
        Objects.requireNonNull(name, "name");
        if (startIndex < 0 || endIndex < startIndex) {
            throw new IllegalArgumentException("Invalid indices: " + startIndex + ".." + endIndex);
        }
    }

    public int length() {
        return endIndex - startIndex;
    }
}