package com.kaua.template.domain.pagination;

import com.kaua.template.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PaginationTest extends UnitTest {

    @Test
    void givenAValidValues_whenCallNewPagination_thenShouldReturnAPagination() {
        final var page = 0;
        final var perPage = 10;
        final var totalPages = 1;
        final var totalItems = 1;
        final var items = List.of(new PaginationDummy("name"));

        final var aMetadata = new PaginationMetadata(page, perPage, totalPages, totalItems);
        final var pagination = new Pagination<>(aMetadata, items);
        final var aResultWithMap = pagination.map(PaginationDummy::getName);

        Assertions.assertEquals(page, pagination.metadata().currentPage());
        Assertions.assertEquals(perPage, pagination.metadata().perPage());
        Assertions.assertEquals(totalPages, pagination.metadata().totalPages());
        Assertions.assertEquals(totalItems, pagination.metadata().totalItems());
        Assertions.assertEquals(items, pagination.items());
        Assertions.assertNotNull(aResultWithMap);
    }

    private record PaginationDummy(String name) {
        public String getName() {
            return name;
        }
    }
}
