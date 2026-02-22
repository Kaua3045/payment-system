package com.payment.system.application.usecases.pixkeys.retrieve.list;

import com.payment.system.application.exceptions.UseCaseInputCannotBeNullException;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.SearchQuery;

import java.util.Objects;

public class DefaultListPixKeysUseCase extends ListPixKeysUseCase {

    private final PixKeyRepository pixKeyRepository;

    public DefaultListPixKeysUseCase(final PixKeyRepository pixKeyRepository, final ApplicationLogger logger) {
        super(logger);
        this.pixKeyRepository = Objects.requireNonNull(pixKeyRepository);
    }

    @Override
    public Pagination<ListPixKeysOutput> execute(SearchQuery input) {
        if (input == null) {
            throw new UseCaseInputCannotBeNullException(ListPixKeysUseCase.class);
        }

        return this.pixKeyRepository.listAll(input)
                .map(ListPixKeysOutput::from);
    }
}
