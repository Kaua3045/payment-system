package com.payment.system;

import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.infrastructure.accounts.AccountJdbcRepository;
import com.payment.system.infrastructure.jdbc.JdbcClientAdapter;
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

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private NamedParameterJdbcOperations operations;

    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        this.accountRepository = new AccountJdbcRepository(new JdbcClientAdapter(jdbcClient, operations));
    }

    protected int countAccounts() {
        return JdbcTestUtils.countRowsInTable(jdbcClient, ACCOUNTS_TABLE);
    }

    protected AccountRepository accountRepository() {
        return accountRepository;
    }
}
