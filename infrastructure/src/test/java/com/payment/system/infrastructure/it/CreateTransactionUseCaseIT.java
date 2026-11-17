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
import com.payment.system.domain.exceptions.InternalErrorException;
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
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

class CreateTransactionUseCaseIT extends AbstractIntegrationTest {

    private static final int THREADS = 100;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    private static final String CONCURRENT_IDEMPOTENCY_KEY = "idempotency-concurrently-test";

    /**
     * Todo resultado possível de uma tentativa de transferência.
     * Cada tentativa cai em EXATAMENTE um deles.
     */
    private enum Outcome {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        CONFLICT,
        ANTI_FRAUD_DENIED,
        ANTI_FRAUD_MANUAL_REVIEW,
        UNEXPECTED
    }

    private final Map<Outcome, LongAdder> outcomes = new EnumMap<>(Outcome.class);

    // "Outcome: motivo" -> quantidade (para saber POR QUE negou / foi para revisão)
    private final Map<String, LongAdder> reasons = new ConcurrentHashMap<>();

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
        for (Outcome outcome : Outcome.values()) {
            outcomes.put(outcome, new LongAdder());
        }
        reasons.clear();
    }

    // ------------------------------------------------------------------
    // TESTES
    // ------------------------------------------------------------------

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

        runConcurrently(THREADS, () ->
                executeTransfer(from, pixKey, AMOUNT, CONCURRENT_IDEMPOTENCY_KEY)
        );

        assertNoUnexpectedFailures(THREADS);

        long persisted = countTransfers(from);
        assertExpectedCount("transfers persisted", 1, persisted);

        assertExpectedCount(
                "transfers persisted with the same idempotency key",
                1,
                countTransfersWithIdempotencyKey(from, CONCURRENT_IDEMPOTENCY_KEY)
        );

        assertBalances(from, to, AMOUNT, AMOUNT, persisted);
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

        runConcurrently(THREADS, () ->
                executeTransfer(from, pixKey, AMOUNT, IdentifierUtils.generateNewId())
        );

        assertNoUnexpectedFailures(THREADS);

        long persisted = countTransfers(from);
        assertExpectedCount("transfers persisted", 1, persisted);
        assertExpectedCount("successful transfers", 1, count(Outcome.SUCCESS));

        assertBalances(from, to, AMOUNT, AMOUNT, persisted);
    }

    @Test
    void shouldPreventNegativeBalance_underHighConcurrency()
            throws InterruptedException {

        // Conta começa com 100.
        // 10 threads tentam transferir 20 simultaneamente.
        //
        // Resultado esperado (sem rejeições do anti-fraude):
        // - exatamente 5 transferências sucedem;
        // - as outras 5 são rejeitadas por saldo insuficiente / conflito;
        // - saldo de origem termina em 0 e o de destino em 100;
        // - saldo nunca fica negativo.
        //
        // Se o anti-fraude rejeitar alguma (deny / manual review), o teste não
        // exige mais 5 sucessos, mas exige consistência total entre
        // transações persistidas e saldos.

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

        runConcurrently(totalThreads, () ->
                executeTransfer(from, pixKey, transferAmount, IdentifierUtils.generateNewId())
        );

        assertNoUnexpectedFailures(totalThreads);

        BigDecimal finalBalanceFrom = ledgerRepository.calculateBalance(from.getId());

        // O saldo nunca pode ser negativo.
        Assertions.assertTrue(
                finalBalanceFrom.compareTo(BigDecimal.ZERO) >= 0,
                "Source balance should never be negative. " + report()
        );

        long persisted = countTransfers(from);

        assertExpectedCount("successful transfers", 5, count(Outcome.SUCCESS));
        assertExpectedCount("transfers persisted", 5, persisted);

        assertBalances(from, to, initialBalance, transferAmount, persisted);
    }

    // ------------------------------------------------------------------
    // EXECUÇÃO / CLASSIFICAÇÃO DOS RESULTADOS
    // ------------------------------------------------------------------

    private void executeTransfer(
            final Account from,
            final PixKey pixKey,
            final BigDecimal amount,
            final String idempotencyKey
    ) {
        try {
            useCase.execute(
                    CreateTransactionCommand.with(
                            from.getId().value().toString(),
                            pixKey.getKey().value(),
                            pixKey.getKey().type().name(),
                            amount,
                            idempotencyKey,
                            "120.57.189.108"
                    )
            );

            record(Outcome.SUCCESS, null);

        } catch (InsufficientFundsException ex) {
            record(Outcome.INSUFFICIENT_FUNDS, null);

        } catch (ConflictException ex) {
            record(Outcome.CONFLICT, null);

        } catch (ValidationException ex) {
            if (containsIgnoreCase(ex.getMessage(), "deny")) {
                record(Outcome.ANTI_FRAUD_DENIED, String.valueOf(ex.getErrors()));
            } else {
                // ValidationException que não seja deny do anti-fraude não é esperada aqui.
                log("Unexpected ValidationException", String.valueOf(ex.getErrors()));
                record(Outcome.UNEXPECTED, "ValidationException: " + ex.getErrors());
            }

        } catch (InternalErrorException ex) {
            if (containsIgnoreCase(ex.getMessage(), "manual review")) {
                record(Outcome.ANTI_FRAUD_MANUAL_REVIEW, ex.getMessage());
            } else {
                log("Unexpected InternalErrorException", ex.getMessage());
                record(Outcome.UNEXPECTED, "InternalErrorException: " + ex.getMessage());
            }

        } catch (Exception ex) {
            log("Unexpected Exception",
                    ex.getClass().getSimpleName() + " - " + ex.getMessage());
            ex.printStackTrace();
            record(Outcome.UNEXPECTED, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void record(final Outcome outcome, final String reason) {
        outcomes.get(outcome).increment();

        if (reason != null) {
            reasons.computeIfAbsent(outcome + " -> " + reason, k -> new LongAdder())
                    .increment();
        }
    }

    private long count(final Outcome outcome) {
        return outcomes.get(outcome).sum();
    }

    private long antiFraudRejections() {
        return count(Outcome.ANTI_FRAUD_DENIED) + count(Outcome.ANTI_FRAUD_MANUAL_REVIEW);
    }

    private static boolean containsIgnoreCase(final String text, final String part) {
        return text != null && text.toLowerCase().contains(part.toLowerCase());
    }

    private static void log(final String title, final String detail) {
        System.out.println("[DEBUG_LOG] " + title + ": " + detail);
    }

    private String report() {
        StringBuilder sb = new StringBuilder("\n===== OUTCOME REPORT =====\n");

        for (Outcome outcome : Outcome.values()) {
            sb.append(String.format("%-26s %d%n", outcome, count(outcome)));
        }

        if (!reasons.isEmpty()) {
            sb.append("--- reasons ---\n");
            reasons.forEach((reason, qty) ->
                    sb.append(String.format("%5d x %s%n", qty.sum(), reason)));
        }

        sb.append("==========================");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // CONCORRÊNCIA
    // ------------------------------------------------------------------

    private void runConcurrently(
            final int threadCount,
            final Runnable action
    ) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    action.run();

                } catch (BrokenBarrierException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    record(Outcome.UNEXPECTED, "Barrier/Interrupted: " + e.getClass().getSimpleName());

                } catch (Throwable t) {
                    // Nada escapa silenciosamente do executor.
                    t.printStackTrace();
                    record(Outcome.UNEXPECTED, "Escaped: " + t.getClass().getSimpleName());

                } finally {
                    done.countDown();
                }
            });
        }

        boolean finished = done.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println(report());

        Assertions.assertTrue(finished, "Concurrent execution timed out. " + report());
    }

    // ------------------------------------------------------------------
    // ASSERTS
    // ------------------------------------------------------------------

    /**
     * Nenhuma exceção inesperada e toda tentativa classificada em algum Outcome.
     * Deny / manual review do anti-fraude NÃO são "inesperados": ficam
     * contabilizados separadamente.
     */
    private void assertNoUnexpectedFailures(final int expectedAttempts) {
        Assertions.assertEquals(
                0,
                count(Outcome.UNEXPECTED),
                "No unexpected exceptions should occur during concurrency test. " + report()
        );

        long total = 0;
        for (Outcome outcome : Outcome.values()) {
            total += count(outcome);
        }

        Assertions.assertEquals(
                expectedAttempts,
                total,
                "Every attempt must end in exactly one known outcome. " + report()
        );
    }

    /**
     * Sem rejeição do anti-fraude: o valor tem que ser exatamente o esperado.
     * Com rejeição do anti-fraude: pode ser menor, mas nunca maior.
     */
    private void assertExpectedCount(
            final String what,
            final long expected,
            final long actual
    ) {
        if (antiFraudRejections() == 0) {
            Assertions.assertEquals(
                    expected,
                    actual,
                    "Unexpected number of " + what + ". " + report()
            );
        } else {
            Assertions.assertTrue(
                    actual <= expected,
                    "Number of " + what + " (" + actual + ") must never exceed " + expected
                            + ". " + report()
            );
        }
    }

    /**
     * Saldos coerentes com o que realmente foi persistido:
     * destino = valor * transferências; origem = saldo inicial - isso.
     */
    private void assertBalances(
            final Account from,
            final Account to,
            final BigDecimal initialBalance,
            final BigDecimal transferAmount,
            final long persistedTransfers
    ) {
        BigDecimal transferred = transferAmount.multiply(BigDecimal.valueOf(persistedTransfers));

        BigDecimal balanceFrom = ledgerRepository.calculateBalance(from.getId());
        BigDecimal balanceTo = ledgerRepository.calculateBalance(to.getId());

        Assertions.assertEquals(
                initialBalance.subtract(transferred).setScale(4),
                balanceFrom.setScale(4),
                "Source balance must match initial balance minus persisted transfers. " + report()
        );

        Assertions.assertEquals(
                transferred.setScale(4),
                balanceTo.setScale(4),
                "Destination must receive exactly what was persisted. " + report()
        );
    }

    private long countTransfers(final Account from) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM transactions
                        WHERE from_account_id = ?
                          AND type = 'TRANSFER'
                        """,
                Integer.class,
                from.getId().value().toString()
        );

        return count == null ? 0 : count;
    }

    private long countTransfersWithIdempotencyKey(final Account from, final String key) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM transactions
                        WHERE from_account_id = ?
                          AND type = 'TRANSFER'
                          AND idempotency_key = ?
                        """,
                Integer.class,
                from.getId().value().toString(),
                key
        );

        return count == null ? 0 : count;
    }

    // ------------------------------------------------------------------
    // FIXTURES
    // ------------------------------------------------------------------

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
                    Money.of(balance),
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
                            Money.of(balance)
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
}
