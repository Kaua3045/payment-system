package com.payment.system;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.application.repositories.PixKeyRepository;
import com.payment.system.application.repositories.TransactionRepository;
import com.payment.system.infrastructure.accounts.AccountJdbcRepository;
import com.payment.system.infrastructure.jdbc.JdbcClientAdapter;
import com.payment.system.infrastructure.pixkeys.PixKeyJdbcRepository;
import com.payment.system.infrastructure.transactions.TransactionJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;

@DataJdbcTest
@Tag("integrationTest")
@ActiveProfiles("test-integration")
public class AbstractRepositoryTest {

    private static final String ACCOUNTS_TABLE = "accounts";
    private static final String PIX_KEYS_TABLE = "pix_keys";
    private static final String TRANSACTIONS_TABLE = "transactions";

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private NamedParameterJdbcOperations operations;

    private AccountRepository accountRepository;
    private PixKeyRepository pixKeyRepository;
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        this.accountRepository = new AccountJdbcRepository(new JdbcClientAdapter(jdbcClient, operations));
        this.pixKeyRepository = new PixKeyJdbcRepository(new JdbcClientAdapter(jdbcClient, operations));
        this.transactionRepository = new TransactionJdbcRepository(new JdbcClientAdapter(jdbcClient, operations));
    }

    protected int countAccounts() {
        return JdbcTestUtils.countRowsInTable(jdbcClient, ACCOUNTS_TABLE);
    }

    protected int countPixKeys() {
        return JdbcTestUtils.countRowsInTable(jdbcClient, PIX_KEYS_TABLE);
    }

    protected int countTransactions() {
        return JdbcTestUtils.countRowsInTable(jdbcClient, TRANSACTIONS_TABLE);
    }

    protected AccountRepository accountRepository() {
        return accountRepository;
    }

    protected PixKeyRepository pixKeyRepository() {
        return pixKeyRepository;
    }

    protected TransactionRepository transactionRepository() {
        return transactionRepository;
    }
}
