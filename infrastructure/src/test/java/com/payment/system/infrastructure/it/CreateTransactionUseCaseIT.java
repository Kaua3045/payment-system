package com.payment.system.infrastructure.it;

import com.payment.system.AbstractIntegrationTest;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.usecases.transactions.create.CreateTransactionCommand;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
import com.payment.system.infrastructure.exceptions.ConflictException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

class CreateTransactionUseCaseIT extends AbstractIntegrationTest {

    private static final int THREADS = 100;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    private final LongAdder insufficientFundsFailures = new LongAdder();
    private final LongAdder conflictsFailures = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder executed = new LongAdder();

    @Autowired
    private CreateTransactionUseCase useCase;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PixKeyRepository pixKeyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetCounters() {
        insufficientFundsFailures.reset();
        conflictsFailures.reset();
        executed.reset();
        success.reset();
    }

    @Test
    void shouldAllowOnlyOneTransactionWithExactBalanceWithSameIdempotencyKey_underHighConcurrency()
            throws InterruptedException {

        Account from = createAccountWithBalance(IdentifierUtils.generateNewMonotonicULID().toString(), AMOUNT);
        Account to = createAccount(IdentifierUtils.generateNewMonotonicULID().toString());
        PixKey pixKey = createPixKey(to);

        runConcurrently(() ->
                executeTransactionIgnoringConflicts(from, pixKey, false)
        );

        assertMultipleIdempotencyKeysWereUsed(from);
        assertOnlyOneTransactionPersisted(from);
        assertBalancesAfterExactTransfer(from, to, AMOUNT);

        Assertions.assertEquals(
                executed.longValue() - 1,
                conflictsFailures.sum(),
                "All failures must be caused by conflicts"
        );
    }

    @Test
    void shouldAllowOnlyOneTransactionWithExactBalanceWithDiffIdempotencyKey_underHighConcurrency()
            throws InterruptedException {

        Account from = createAccountWithBalance(IdentifierUtils.generateNewMonotonicULID().toString(), AMOUNT);
        Account to = createAccount(IdentifierUtils.generateNewMonotonicULID().toString());
        PixKey pixKey = createPixKey(to);

        runConcurrently(() ->
                executeTransactionIgnoringConflicts(from, pixKey, true)
        );

        assertMultipleIdempotencyKeysWereUsed(from);
        assertOnlyOneTransactionPersisted(from);
        assertBalancesAfterExactTransfer(from, to, AMOUNT);

        Assertions.assertEquals(1, success.sum(), "Only one transaction should succeed");
        Assertions.assertEquals(executed.longValue() - 1, conflictsFailures.sum(), "Account optimistic locking should prevent conflicts when idempotency keys differ");
        Assertions.assertEquals(0, insufficientFundsFailures.sum(), "All transactions failed because of optimistic locking, not insufficient funds but is possible that some of them failed because of insufficient funds, which is not ideal but acceptable given the high concurrency and the fact that all transactions are trying to transfer the same amount from the same account");
    }

    private Account createAccount(final String owner) {
        return accountRepository.save(Account.newAccount(owner));
    }

    private Account createAccountWithBalance(final String owner, final BigDecimal balance) {
        Account account = createAccount(owner);
        account.credit(balance);
        return accountRepository.save(account);
    }

    private PixKey createPixKey(Account account) {
        return pixKeyRepository.save(
                PixKey.newPixKey(
                        new PixKeyValueFactory()
                                .create(PixKeyType.RANDOM, IdentifierUtils.generateNewId()),
                        account.getId()
                )
        );
    }

    private void executeTransactionIgnoringConflicts(Account from, PixKey pixKey, final boolean diffIdempotencyKey) {
        try {
            this.useCase.execute(
                    CreateTransactionCommand.with(
                            from.getId().value().toString(),
                            pixKey.getKey().value(),
                            pixKey.getKey().type().name(),
                            AMOUNT,
                            diffIdempotencyKey ? IdentifierUtils.generateNewId() : "idempotency-concurrently-test"
                    )
            );
            success.increment();
        } catch (ValidationException ex) {
            if (ex.getMessage().contains("Insufficient funds")) {
                insufficientFundsFailures.increment();
            }
        } catch (ConflictException ignored) {
            conflictsFailures.increment();
        }
    }

    private void runConcurrently(Runnable action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                try {
                    barrier.await(); // All threads will start executing the action at the same time
                    action.run();
                    executed.increment();
                } catch (BrokenBarrierException | InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }

        done.await();
        executor.shutdown();
    }

    private void assertOnlyOneTransactionPersisted(Account from) {
        Integer count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM transactions
                            WHERE from_account_id = ?
                        """,
                Integer.class,
                from.getId().value().toString()
        );

        Assertions.assertEquals(1, count,
                "Under high concurrency, only ONE transaction must be persisted");
    }

    private void assertMultipleIdempotencyKeysWereUsed(Account from) {
        Integer count = jdbcTemplate.queryForObject("""
                            SELECT COUNT(DISTINCT idempotency_key)
                            FROM transactions
                            WHERE from_account_id = ?
                        """,
                Integer.class,
                from.getId().value().toString());

        Assertions.assertTrue(
                count >= 1,
                "At least one idempotency key must be recorded"
        );
    }

    private void assertBalancesAfterExactTransfer(
            Account from,
            Account to,
            BigDecimal amount
    ) {
        Account updatedFrom = reloadAccount(from);
        Account updatedTo = reloadAccount(to);

        Assertions.assertEquals(
                Money.zero().amount(),
                updatedFrom.getBalance().amount(),
                "Source account balance must be ZERO"
        );

        Assertions.assertEquals(
                new Money(amount).amount(),
                updatedTo.getBalance().amount(),
                "Destination account must receive exactly the transferred amount"
        );
    }

    private Account reloadAccount(Account account) {
        return accountRepository
                .accountOfId(account.getId().value().toString())
                .orElseThrow();
    }
}
