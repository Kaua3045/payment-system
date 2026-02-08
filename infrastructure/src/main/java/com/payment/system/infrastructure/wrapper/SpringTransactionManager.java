package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.utils.Generated;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Generated
@Component
public class SpringTransactionManager implements TransactionManager {

    private final PlatformTransactionManager txManager;

    public SpringTransactionManager(PlatformTransactionManager txManager) {
        this.txManager = txManager;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return template.execute(status -> action.get());
    }
}
