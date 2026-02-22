package com.payment.system.application.usecases.pixkeys.retrieve.list;

import com.payment.system.application.UseCase;
import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.pagination.Pagination;
import com.payment.system.domain.pagination.SearchQuery;

public abstract class ListPixKeysUseCase extends UseCase<SearchQuery, Pagination<ListPixKeysOutput>> {

    protected ListPixKeysUseCase(ApplicationLogger aLogger) {
        super(aLogger);
    }
}
