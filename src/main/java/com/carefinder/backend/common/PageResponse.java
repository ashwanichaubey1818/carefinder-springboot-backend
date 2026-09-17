package com.carefinder.backend.common;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> of(List<T> allItems, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int from = Math.min(safePage * safeSize, allItems.size());
        int to = Math.min(from + safeSize, allItems.size());
        int totalPages = allItems.isEmpty() ? 0 : (int) Math.ceil((double) allItems.size() / safeSize);
        return new PageResponse<>(
                List.copyOf(allItems.subList(from, to)),
                safePage,
                safeSize,
                allItems.size(),
                totalPages,
                safePage == 0,
                totalPages == 0 || safePage >= totalPages - 1
        );
    }
}
