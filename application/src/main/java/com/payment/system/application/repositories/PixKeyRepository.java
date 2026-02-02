package com.payment.system.application.repositories;

import com.payment.system.domain.pixkeys.PixKey;

import java.util.Optional;

public interface PixKeyRepository {

    PixKey save(PixKey pixKey);

    boolean existsByValue(String value);

    Optional<PixKey> pixKeyOfActiveByValue(String value);
}
