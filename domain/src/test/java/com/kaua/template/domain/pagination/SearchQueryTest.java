package com.kaua.template.domain.pagination;

import com.kaua.template.domain.UnitTest;
import com.kaua.template.domain.exceptions.ValidationException;
import com.kaua.template.domain.utils.Period;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class SearchQueryTest extends UnitTest {

    @Test
    void givenAValidValues_whenCallNewSearchQuery_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "";
        final var sort = "createdAt";
        final var direction = "asc";

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertTrue(aQuery.getPeriod().isEmpty());
    }

    @Test
    void givenAValidValues_whenCallNewSearchQueryWithPeriod_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "";
        final var sort = "createdAt";
        final var direction = "asc";
        final var period = new Period("2021-01-01T00:00:00Z", "2021-01-31T23:59:59Z");

        final var aQuery = new SearchQuery(page, perPage, terms, sort, direction, period, null);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertEquals(period, aQuery.period());
    }

    @Test
    void givenAValidAllValues_whenCallNewSearchQuery_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var period = new Period("2021-01-01T00:00:00Z", "2021-01-31T23:59:59Z");
        final var filters = Map.of("status", "active");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, period, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertEquals(period, aQuery.period());
        Assertions.assertEquals(filters, aQuery.filters());
    }

    @Test
    void givenAValidValues_whenCallNewSearchQueryWithFilters_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var filters = Map.of("status", "active");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertTrue(aQuery.getPeriod().isEmpty());
        Assertions.assertEquals(filters, aQuery.filters());
    }

    @Test
    void givenAValidValues_whenCallNewSearchQueryWithPeriodAndFilters_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var period = new Period("2021-01-01T00:00:00Z", "2021-01-31T23:59:59Z");
        final var filters = Map.of("status", "active");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, period, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertEquals(period, aQuery.period());
        Assertions.assertEquals(filters, aQuery.filters());
    }

    @Test
    void givenAValidValues_whenCallNewSearchQueryWithPeriodAndNoFilters_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var period = new Period("2021-01-01T00:00:00Z", "2021-01-31T23:59:59Z");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, period);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertEquals(period, aQuery.period());
        Assertions.assertTrue(aQuery.filters().isEmpty());
    }

    @Test
    void givenAnInvalidCleanFilters_whenCallNewSearchQuery_shouldThrowsException() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var filters = Map.of("page", "1");

        final var expectedProperty = "filters";
        final var expectedErrorMessage = "use static factory method to create SearchQuery with filters";

        final var aException = Assertions.assertThrows(ValidationException.class, () ->
                new SearchQuery(page, perPage, terms, sort, direction, null, filters));

        Assertions.assertEquals(expectedProperty, aException.getErrors().getFirst().property());
        Assertions.assertEquals(expectedErrorMessage, aException.getErrors().getFirst().message());
    }

    @Test
    void givenAValidValuesWithFiltersAndPrefixIsFilters_whenCallNewSearchQuery_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var filters = Map.of("filters.status", "active");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertTrue(aQuery.getPeriod().isEmpty());
        Assertions.assertEquals(Map.of("status", "active"), aQuery.filters());
    }

    @Test
    void givenAValidValuesWithFiltersDuplicated_whenCallNewSearchQuery_shouldReturnASearchQueryInstance() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var filters = Map.of("filters.status", "active", "status", "active");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertTrue(aQuery.getPeriod().isEmpty());
        Assertions.assertEquals(Map.of("status", "active"), aQuery.filters());
    }

    @Test
    void givenAnInvalidPermittedFilter_whenCallNewSearchQuery_shouldReturnEmpty() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var filters = Map.of("page", "value");

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertTrue(aQuery.getPeriod().isEmpty());
        Assertions.assertTrue(aQuery.filters().isEmpty());
    }

    @Test
    void givenAnInvalidNullFilterValue_whenCallNewSearchQuery_shouldReturnEmpty() {
        final var page = 0;
        final var perPage = 10;
        final var terms = "test";
        final var sort = "createdAt";
        final var direction = "asc";
        final var filters = new HashMap<String, String>();
        filters.put("status", null); // Invalid filter value

        final var aQuery = SearchQuery.newSearchQuery(page, perPage, terms, sort, direction, filters);

        Assertions.assertEquals(page, aQuery.page());
        Assertions.assertEquals(perPage, aQuery.perPage());
        Assertions.assertEquals(terms, aQuery.terms());
        Assertions.assertEquals(sort, aQuery.sort());
        Assertions.assertEquals(direction, aQuery.direction());
        Assertions.assertTrue(aQuery.getPeriod().isEmpty());
        Assertions.assertTrue(aQuery.filters().isEmpty());
    }
}
