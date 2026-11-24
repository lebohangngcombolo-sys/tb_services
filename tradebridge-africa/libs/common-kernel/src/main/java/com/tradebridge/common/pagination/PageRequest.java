package com.tradebridge.common.pagination;

import com.tradebridge.common.enums.SortDirection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Validated, bounded pagination request.
 *
 * <p>Rules:
 * <ul>
 *   <li>{@code page} must be >= 0.</li>
 *   <li>{@code size} must be >= 1 and <= {@link #MAX_PAGE_SIZE} (default 100).</li>
 *   <li>{@code sorts} is never null; empty means "use service default".</li>
 * </ul>
 */
public final class PageRequest {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final int page;
    private final int size;
    private final List<SortSpec> sorts;

    private PageRequest(int page, int size, List<SortSpec> sorts) {
        this.page = page;
        this.size = size;
        this.sorts = sorts;
    }

    public static PageRequest of(int page, int size) {
        return of(page, size, Collections.emptyList());
    }

    public static PageRequest of(int page, int size, List<SortSpec> sorts) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        int effective = Math.min(size, MAX_PAGE_SIZE);
        return new PageRequest(page, effective, sorts == null ? Collections.emptyList() : List.copyOf(sorts));
    }

    public static PageRequest of(int page, int size, String sortField, SortDirection direction) {
        if (sortField == null || sortField.isBlank()) {
            return of(page, size);
        }
        return of(page, size, List.of(SortSpec.of(sortField, direction)));
    }

    public static PageRequest defaults() {
        return of(0, DEFAULT_PAGE_SIZE);
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public List<SortSpec> getSorts() { return sorts; }

    public int offset() {
        return page * size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageRequest other)) return false;
        return page == other.page && size == other.size && Objects.equals(sorts, other.sorts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, sorts);
    }

    @Override
    public String toString() {
        return "PageRequest{page=" + page + ", size=" + size + ", sorts=" + sorts + '}';
    }
}