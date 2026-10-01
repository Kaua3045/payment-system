package com.payment.system.infrastructure.gateways;

import com.payment.system.application.gateways.AccountGateway;
import com.payment.system.application.repositories.AccountRepository;
import com.payment.system.domain.accounts.Account;
import com.payment.system.domain.exceptions.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AccountGatewayImpl implements AccountGateway {

    private final AccountRepository accountRepository;

    public AccountGatewayImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountResponse accountOfId(final String accountId) {
        final var aAccount = this.accountRepository.accountOfId(accountId)
                .orElseThrow(NotFoundException.with(Account.class, accountId));

        return new AccountResponse(
                aAccount.getStatus(),
                aAccount.getId()
        );
    }
}
