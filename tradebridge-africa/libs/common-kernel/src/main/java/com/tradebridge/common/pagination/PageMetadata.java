package com.tradebridge.common.pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response-side page descriptor, paired with {@code PageResponse}.
 */
public record PageMetadata(
        @JsonProperty("number") int number,
        @JsonProperty("size") int size,
        @JsonProperty("totalElements") long totalElements,
        @JsonProperty("totalPages") int totalPages,
        @JsonProperty("first") boolean first,
        @JsonProperty("last") boolean last
) {
    public static PageMetadata of(int number, int size, long totalElements) {
        if (number < 0) throw new IllegalArgumentException("number must be >= 0");
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        if (totalElements < 0) throw new IllegalArgumentException("totalElements must be >= 0");
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean first = number == 0;
        boolean last = number >= Math.max(totalPages - 1, 0);
        return new PageMetadata(number, size, totalElements, totalPages, first, last);
    }
}