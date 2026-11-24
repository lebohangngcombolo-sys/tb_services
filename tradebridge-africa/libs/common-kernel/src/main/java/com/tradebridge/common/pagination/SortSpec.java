package com.tradebridge.common.pagination;

import com.tradebridge.common.enums.SortDirection;

import java.util.Objects;
import java.util.Set;

/**
 * A single sort field + direction. Field name must be present in the
 * caller-supplied allow-list — services should not let clients sort by
 * arbitrary columns.
 */
public record SortSpec(String field, SortDirection direction) {

    public SortSpec {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(direction, "direction");
    }

    public static SortSpec of(String field, SortDirection direction) {
        return new SortSpec(field, direction);
    }

    public static SortSpec safe(String field, SortDirection direction, Set<String> allowList, SortSpec fallback) {
        if (field == null || allowList == null || !allowList.contains(field)) {
            return fallback;
        }
        return new SortSpec(field, direction == null ? SortDirection.ASC : direction);
    }
}