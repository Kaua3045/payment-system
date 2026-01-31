package com.payment.system.application.repositories;

import com.payment.system.domain.accounts.Account;

public interface AccountRepository {

    Account save(Account anAccount);
}
