package com.payment.system.application.repositories;

import com.payment.system.domain.pixkeys.PixKey;

public interface PixKeyRepository {

    PixKey save(PixKey pixKey);

    boolean existsByValue(String value);
}
