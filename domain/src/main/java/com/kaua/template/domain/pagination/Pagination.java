package com.kaua.template.domain.pagination;

import java.util.List;
import java.util.function.Function;

public record Pagination<T>(
        PaginationMetadata metadata,
        List<T> items
) {

    public <R> Pagination<R> map(final Function<T, R> mapper) {
        final List<R> aNewList = this.items.stream().map(mapper).toList();

        return new Pagination<>(
                metadata(),
                aNewList
        );
    }
}