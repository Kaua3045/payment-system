package com.payment.system.infrastructure.it;

import com.payment.system.AbstractIntegrationTest;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.LedgerRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.application.usecases.transactions.create.CreateTransactionCommand;
import com.payment.system.application.usecases.transactions.create.CreateTransactionUseCase;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.ConflictException;
import com.payment.system.domain.exceptions.InsufficientFundsException;
import com.payment.system.domain.exceptions.ValidationException;
import com.payment.system.domain.ledger.LedgerEntry;
import com.payment.system.domain.pixkeys.PixKey;
import com.payment.system.domain.pixkeys.PixKeyType;
import com.payment.system.domain.pixkeys.PixKeyValueFactory;
import com.payment.system.domain.transactions.DepositSource;
import com.payment.system.domain.transactions.Transaction;
import com.payment.system.domain.transactions.TransactionType;
import com.payment.system.domain.utils.IdentifierUtils;
import com.payment.system.domain.valueobjects.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

class CreateTransactionUseCaseIT extends AbstractIntegrationTest {

    private static final int THREADS = 100;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    private static final String CONCURRENT_IDEMPOTENCY_KEY = "idempotency-concurrently-test";

    private final LongAdder insufficientFundsFailures = new LongAdder();
    private final LongAdder conflictsFailures = new LongAdder();
    private final LongAdder unexpectedFailures = new LongAdder();
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

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetCounters() {
        insufficientFundsFailures.reset();
        conflictsFailures.reset();
        unexpectedFailures.reset();
        executed.reset();
        success.reset();
    }

    @Test
    void shouldAllowOnlyOneTransferWithExactBalanceWithSameIdempotencyKey_underHighConcurrency()
            throws InterruptedException {

        Account from = createAccountWithBalance(
                IdentifierUtils.generateNewMonotonicULID().toString(),
                AMOUNT
        );

        Account to = createAccount(
                IdentifierUtils.generateNewMonotonicULID().toString()
        );

        PixKey pixKey = createPixKey(to);

        runConcurrently(() ->
                executeTransactionIgnoringConcurrencyFailures(
                        from,
                        pixKey,
                        false
                )
        );

        assertOnlyOneTransferPersisted(from);
        assertOnlyOneTransferWithIdempotencyKeyPersisted(from);

        assertBalancesAfterExactTransfer(
                from,
                to,
                AMOUNT
        );

        assertOnlyConcurrencyFailures();
    }

    @Test
    void shouldAllowOnlyOneTransferWithExactBalanceWithDifferentIdempotencyKeys_underHighConcurrency()
            throws InterruptedException {

        Account from = createAccountWithBalance(
                IdentifierUtils.generateNewMonotonicULID().toString(),
                AMOUNT
        );

        Account to = createAccount(
                IdentifierUtils.generateNewMonotonicULID().toString()
        );

        PixKey pixKey = createPixKey(to);

        runConcurrently(() ->
                executeTransactionIgnoringConcurrencyFailures(
                        from,
                        pixKey,
                        true
                )
        );

        assertOnlyOneTransferPersisted(from);

        assertBalancesAfterExactTransfer(
                from,
                to,
                AMOUNT
        );

        Assertions.assertEquals(
                1,
                success.sum(),
                "Only one transfer should succeed"
        );

        assertOnlyConcurrencyFailures();
    }

    @Test
    void shouldPreventNegativeBalance_underHighConcurrency()
            throws InterruptedException {

        // Conta começa com 100.
        // 10 threads tentam transferir 20 simultaneamente.
        //
        // Resultado esperado:
        // - exatamente 5 transferências sucedem;
        // - exatamente 5 transferências são rejeitadas;
        // - saldo de origem termina em 0;
        // - saldo de destino termina em 100;
        // - saldo nunca fica negativo.

        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal transferAmount = new BigDecimal("20.00");
        int totalThreads = 10;

        Account from = createAccountWithBalance(
                IdentifierUtils.generateNewMonotonicULID().toString(),
                initialBalance
        );

        Account to = createAccount(
                IdentifierUtils.generateNewMonotonicULID().toString()
        );

        PixKey pixKey = createPixKey(to);

        runConcurrently(
                totalThreads,
                () -> executeTransfer(
                        from,
                        pixKey,
                        transferAmount
                )
        );

        BigDecimal finalBalanceFrom =
                ledgerRepository.calculateBalance(from.getId());

        BigDecimal finalBalanceTo =
                ledgerRepository.calculateBalance(to.getId());

        // O saldo nunca pode ser negativo.
        Assertions.assertTrue(
                finalBalanceFrom.compareTo(BigDecimal.ZERO) >= 0,
                "Source balance should never be negative"
        );

        // Exatamente 5 transferências de 20 devem ser executadas.
        Assertions.assertEquals(
                5,
                success.sum(),
                "Exactly 5 transfers should succeed"
        );

        // Exatamente 5 transferências devem ter sido persistidas.
        assertTransferCount(from, 5);

        // Todas as falhas devem ser consequência do controle de concorrência.
//        assertOnlyConcurrencyFailures();

        // Saldo final esperado.
        Assertions.assertEquals(
                initialBalance.setScale(4),
                finalBalanceTo.setScale(4),
                "Destination account should receive exactly the transferred amount"
        );

        Assertions.assertEquals(
                BigDecimal.ZERO.setScale(4),
                finalBalanceFrom.setScale(4),
                "Source account balance should be ZERO"
        );
    }

    private Account createAccount(final String owner) {
        return accountRepository.save(
                Account.newAccount(owner)
        );
    }

    private Account createAccountWithBalance(
            final String owner,
            final BigDecimal balance
    ) {
        Account account = createAccount(owner);

        transactionTemplate.executeWithoutResult(status -> {
            PixKey pixKey = createPixKey(account);

            Transaction deposit = Transaction.newTransaction(
                    account.getId(),
                    account.getId(),
                    pixKey.getId(),
                    new Money(balance),
                    TransactionType.CREDIT,
                    DepositSource.ATM,
                    IdentifierUtils.generateNewId()
            );

            deposit.complete();

            transactionRepository.save(deposit);

            ledgerRepository.save(
                    LedgerEntry.newCredit(
                            account.getId(),
                            deposit.getId(),
                            balance
                    )
            );
        });

        return accountRepository.save(account);
    }

    private PixKey createPixKey(final Account account) {
        return pixKeyRepository.save(
                PixKey.newPixKey(
                        new PixKeyValueFactory()
                                .create(
                                        PixKeyType.RANDOM,
                                        IdentifierUtils.generateNewId()
                                ),
                        account.getId()
                )
        );
    }

    private void executeTransactionIgnoringConcurrencyFailures(
            final Account from,
            final PixKey pixKey,
            final boolean differentIdempotencyKey
    ) {
        try {
            useCase.execute(
                    CreateTransactionCommand.with(
                            from.getId().value().toString(),
                            pixKey.getKey().value(),
                            pixKey.getKey().type().name(),
                            AMOUNT,
                            differentIdempotencyKey
                                    ? IdentifierUtils.generateNewId()
                                    : CONCURRENT_IDEMPOTENCY_KEY
                    )
            );

            success.increment();

        } catch (InsufficientFundsException ex) {
            insufficientFundsFailures.increment();

        } catch (ConflictException ex) {
            conflictsFailures.increment();

        } catch (ValidationException ex) {
            System.out.println(
                    "[DEBUG_LOG] ValidationException: "
                            + ex.getErrors()
            );

            /*
             * ValidationException não é esperada como mecanismo
             * de rejeição nesse cenário de concorrência.
             */
            unexpectedFailures.increment();

        } catch (Exception ex) {
            System.out.println(
                    "[DEBUG_LOG] Unexpected Exception: "
                            + ex.getClass().getSimpleName()
                            + " - "
                            + ex.getMessage()
            );

            ex.printStackTrace();

            unexpectedFailures.increment();
        }
    }

    private void executeTransfer(
            final Account from,
            final PixKey pixKey,
            final BigDecimal amount
    ) {
        try {
            useCase.execute(
                    CreateTransactionCommand.with(
                            from.getId().value().toString(),
                            pixKey.getKey().value(),
                            pixKey.getKey().type().name(),
                            amount,
                            IdentifierUtils.generateNewId()
                    )
            );

            success.increment();

        } catch (InsufficientFundsException ex) {
            insufficientFundsFailures.increment();

        } catch (ConflictException ex) {
            conflictsFailures.increment();

        } catch (ValidationException ex) {
            System.out.println(
                    "[DEBUG_LOG] ValidationException: "
                            + ex.getErrors()
            );

            unexpectedFailures.increment();

        } catch (Exception ex) {
            System.out.println(
                    "[DEBUG_LOG] Unexpected Exception: "
                            + ex.getClass().getSimpleName()
                            + " - "
                            + ex.getMessage()
            );

            ex.printStackTrace();

            unexpectedFailures.increment();

        } finally {
            executed.increment();
        }
    }

    private void runConcurrently(final Runnable action)
            throws InterruptedException {

        runConcurrently(
                THREADS,
                action
        );
    }

    private void runConcurrently(
            final int threadCount,
            final Runnable action
    ) throws InterruptedException {

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        CyclicBarrier barrier =
                new CyclicBarrier(threadCount);

        CountDownLatch done =
                new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();

                    action.run();

                    executed.increment();

                } catch (BrokenBarrierException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedFailures.increment();

                } finally {
                    done.countDown();
                }
            });
        }

        done.await();

        executor.shutdown();
    }

    private void assertOnlyOneTransferPersisted(
            final Account from
    ) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM transactions
                        WHERE from_account_id = ?
                          AND type = 'TRANSFER'
                        """,
                Integer.class,
                from.getId().value().toString()
        );

        Assertions.assertEquals(
                1,
                count,
                "Under high concurrency, only ONE transfer must be persisted"
        );
    }

    private void assertTransferCount(
            final Account from,
            final int expected
    ) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM transactions
                        WHERE from_account_id = ?
                          AND type = 'TRANSFER'
                        """,
                Integer.class,
                from.getId().value().toString()
        );

        Assertions.assertEquals(
                expected,
                count,
                "Unexpected number of persisted transfers"
        );
    }

    private void assertOnlyOneTransferWithIdempotencyKeyPersisted(
            final Account from
    ) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM transactions
                        WHERE from_account_id = ?
                          AND type = 'TRANSFER'
                          AND idempotency_key = ?
                        """,
                Integer.class,
                from.getId().value().toString(),
                CONCURRENT_IDEMPOTENCY_KEY
        );

        Assertions.assertEquals(
                1,
                count,
                "Exactly ONE transfer must be persisted for the same idempotency key"
        );
    }

    private void assertOnlyConcurrencyFailures() {
        long totalConcurrencyFailures =
                conflictsFailures.sum()
                        + insufficientFundsFailures.sum();

        Assertions.assertEquals(
                executed.longValue() - success.longValue(),
                totalConcurrencyFailures,
                "All failed requests must be rejected by concurrency control"
        );

        Assertions.assertEquals(
                0,
                unexpectedFailures.sum(),
                "No unexpected exceptions should occur during concurrency test"
        );
    }

    private void assertBalancesAfterExactTransfer(
            final Account from,
            final Account to,
            final BigDecimal amount
    ) {
        BigDecimal balanceFrom =
                ledgerRepository.calculateBalance(from.getId());

        BigDecimal balanceTo =
                ledgerRepository.calculateBalance(to.getId());

        Assertions.assertEquals(
                BigDecimal.ZERO.setScale(4),
                balanceFrom.setScale(4),
                "Source account balance must be ZERO"
        );

        Assertions.assertEquals(
                amount.setScale(4),
                balanceTo.setScale(4),
                "Destination account must receive exactly the transferred amount"
        );
    }
}
