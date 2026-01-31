package com.payment.system.application.repositories;

import com.payment.system.domain.accounts.Account;

import java.util.Optional;

public interface AccountRepository {

    Account save(Account anAccount);

    Optional<Account> accountOfId(String anId);
}
