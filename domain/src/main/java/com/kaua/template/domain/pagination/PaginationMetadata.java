package com.kaua.template.domain.pagination;

public record PaginationMetadata(
        int currentPage,
        int perPage,
        int totalPages,
        long totalItems
) {
}
