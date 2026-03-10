package com.payment.system.application.repositories;

import com.payment.system.domain.accounts.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account anAccount);

    void applyTransfer(Account fromAccount, Account toAccount);

    Optional<Account> accountOfId(String anId);
}
