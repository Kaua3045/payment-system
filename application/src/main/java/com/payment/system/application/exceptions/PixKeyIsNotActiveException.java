package com.payment.system.domain.exceptions;

import java.util.ArrayList;

public class PixKeyIsNotActiveException extends DomainException {

    public PixKeyIsNotActiveException(final String pixKey) {
        super("PixKey %s is not active".formatted(pixKey), new ArrayList<>(), "pix_key_inactive");
    }
}
