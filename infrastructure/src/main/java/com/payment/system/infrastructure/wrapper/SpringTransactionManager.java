package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.TracerWrapper;
import com.payment.system.application.wrapper.TransactionManager;
import com.payment.system.domain.utils.Generated;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Generated
@Component
public class SpringTransactionManager implements TransactionManager {

    private final PlatformTransactionManager txManager;
    private final TracerWrapper tracer;

    public SpringTransactionManager(PlatformTransactionManager txManager, TracerWrapper tracer) {
        this.txManager = txManager;
        this.tracer = tracer;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return tracer.traceWithReturn("database.transaction", (span) -> {
            final long transactionStart = System.nanoTime();

            TransactionStatus status =
                    txManager.getTransaction(
                            new DefaultTransactionDefinition(
                                    TransactionDefinition.PROPAGATION_REQUIRED
                            )
                    );

            final long transactionAcquired =
                    System.nanoTime();

            final long acquireMs =
                    (transactionAcquired - transactionStart)
                            / 1_000_000;

            span.setAttribute(
                    "db.transaction.acquire_ms",
                    acquireMs
            );

            try {
                final long bodyStart =
                        System.nanoTime();

                T result = action.get();

                final long bodyMs =
                        (System.nanoTime() - bodyStart)
                                / 1_000_000;

                span.setAttribute(
                        "db.transaction.body_ms",
                        bodyMs
                );

                final long commitStart =
                        System.nanoTime();

                txManager.commit(status);

                final long commitMs =
                        (System.nanoTime() - commitStart)
                                / 1_000_000;

                span.setAttribute(
                        "db.transaction.commit_ms",
                        commitMs
                );

                return result;
            } catch (Exception e) {
                final long rollbackStart =
                        System.nanoTime();

                txManager.rollback(status);

                final long rollbackMs =
                        (System.nanoTime() - rollbackStart)
                                / 1_000_000;

                span.setAttribute(
                        "db.transaction.rollback_ms",
                        rollbackMs
                );

                throw e;
            }
        });
    }
}
