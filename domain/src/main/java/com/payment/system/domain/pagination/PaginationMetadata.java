package com.payment.system.domain.pagination;

public record PaginationMetadata(
        int currentPage,
        int perPage,
        int totalPages,
        long totalItems
) {
}
