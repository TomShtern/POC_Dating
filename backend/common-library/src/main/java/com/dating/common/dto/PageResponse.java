package com.dating.common.dto;

import java.util.List;

/**
 * Generic paginated response DTO.
 * Wraps a list of items with pagination metadata.
 *
 * @param <T> Type of items in the page
 * @param content List of items for the current page
 * @param page Current page number (0-indexed)
 * @param size Number of items per page
 * @param totalElements Total number of items across all pages
 * @param totalPages Total number of pages
 * @param first Whether this is the first page
 * @param last Whether this is the last page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /**
     * Create a PageResponse from raw pagination data.
     *
     * @param <T> Type of items
     * @param content List of items for the current page
     * @param page Current page number (0-indexed)
     * @param size Number of items per page
     * @param totalElements Total number of items across all pages
     * @return New PageResponse instance
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;
        return new PageResponse<>(content, page, size, totalElements, totalPages, first, last);
    }

    /**
     * Create an empty PageResponse.
     *
     * @param <T> Type of items
     * @param page Current page number
     * @param size Page size
     * @return Empty PageResponse instance
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0L, 0, true, true);
    }

    /**
     * Check if the page has content.
     *
     * @return true if content is not empty
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * Check if there is a next page.
     *
     * @return true if there is a next page
     */
    public boolean hasNext() {
        return !last;
    }

    /**
     * Check if there is a previous page.
     *
     * @return true if there is a previous page
     */
    public boolean hasPrevious() {
        return !first;
    }
}
