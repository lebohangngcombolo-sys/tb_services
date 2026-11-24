package com.tradebridge.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tradebridge.common.pagination.PageMetadata;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Standard paged payload used as the {@code data} field of {@link ApiResponse}.
 *
 * <pre>
 * {
 *   "content": [ ... ],
 *   "page": { "number": 0, "size": 20, "totalElements": 42, "totalPages": 3, "first": true, "last": false }
 * }
 * </pre>
 */
public final class PageResponse<T> {

    @JsonProperty("content")
    private final List<T> content;

    @JsonProperty("page")
    private final PageMetadata page;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    private PageResponse(List<T> content, PageMetadata page, Instant timestamp) {
        this.content = content == null ? Collections.emptyList() : List.copyOf(content);
        this.page = Objects.requireNonNull(page, "page");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public static <T> PageResponse<T> of(List<T> content, PageMetadata page) {
        return new PageResponse<>(content, page, Instant.now());
    }

    public static <T> PageResponse<T> empty(PageMetadata page) {
        return new PageResponse<>(Collections.emptyList(), page, Instant.now());
    }

    public List<T> getContent() { return content; }
    public PageMetadata getPage() { return page; }
    public Instant getTimestamp() { return timestamp; }
}