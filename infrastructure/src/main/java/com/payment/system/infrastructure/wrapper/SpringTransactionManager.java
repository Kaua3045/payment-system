package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.TransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringTransactionManager implements TransactionManager {

    private final PlatformTransactionManager txManager;

    public SpringTransactionManager(PlatformTransactionManager txManager) {
        this.txManager = txManager;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(txManager);
        return template.execute(status -> action.get());
    }
}
