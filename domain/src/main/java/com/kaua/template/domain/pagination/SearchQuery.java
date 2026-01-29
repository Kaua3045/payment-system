package com.kaua.template.domain.pagination;

import com.kaua.template.domain.utils.Period;
import com.kaua.template.domain.validation.AssertionConcern;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record SearchQuery(
        int page,
        int perPage,
        String terms,
        String sort,
        String direction,
        Period period,
        Map<String, String> filters
) implements AssertionConcern {

    private static final List<String> NOT_PERMITTED_PARAMS = List.of(
            "page",
            "perPage",
            "terms",
            "sort",
            "direction",
            "period"
    );

    public SearchQuery {
        if (filters != null) {
            this.assertArgumentTrue(filters.entrySet()
                            .stream()
                            .noneMatch(it -> NOT_PERMITTED_PARAMS.contains(it.getKey())),
                    "filters",
                    "use static factory method to create SearchQuery with filters");
        }
    }

    public static SearchQuery newSearchQuery(
            int page,
            int perPage,
            String terms,
            String sort,
            String direction
    ) {
        return new SearchQuery(
                page,
                perPage,
                terms,
                sort,
                direction,
                null,
                Map.of()
        );
    }

    public static SearchQuery newSearchQuery(
            int page,
            int perPage,
            String terms,
            String sort,
            String direction,
            Map<String, String> filters
    ) {
        return new SearchQuery(
                page,
                perPage,
                terms,
                sort,
                direction,
                null,
                cleanFilters(filters)
        );
    }

    public static SearchQuery newSearchQuery(
            int page,
            int perPage,
            String terms,
            String sort,
            String direction,
            Period period,
            Map<String, String> filters
    ) {
        return new SearchQuery(
                page,
                perPage,
                terms,
                sort,
                direction,
                period,
                cleanFilters(filters)
        );
    }

    public static SearchQuery newSearchQuery(
            int page,
            int perPage,
            String terms,
            String sort,
            String direction,
            Period period
    ) {
        return new SearchQuery(
                page,
                perPage,
                terms,
                sort,
                direction,
                period,
                Map.of()
        );
    }

    public Optional<Period> getPeriod() {
        return Optional.ofNullable(period);
    }

    private static Map<String, String> cleanFilters(Map<String, String> filters) {
        return filters.entrySet().stream()
                .filter(it -> !NOT_PERMITTED_PARAMS.contains(it.getKey()))
                .filter(it -> !(it.getValue() == null))
                .collect(Collectors.toMap(
                        e -> e.getKey().startsWith("filters.")
                                ? e.getKey().substring("filters.".length())
                                : e.getKey(),
                        Map.Entry::getValue,
                        (v1, v2) -> v1 // in case of duplicate keys, keep the first value
                ));
    }
}
