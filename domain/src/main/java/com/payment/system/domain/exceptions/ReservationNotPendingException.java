package com.payment.system.domain.exceptions;

import java.util.ArrayList;

public class ReservationNotPendingException extends DomainException {

    public ReservationNotPendingException() {
        super("Reservation is not pending", new ArrayList<>(), "reservation_not_pending");
    }
}
