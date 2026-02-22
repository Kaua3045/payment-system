package com.payment.system.application.helpers;

public enum ErrorType {

    BUSINESS,
    UNEXPECTED;

    public static boolean IsBusiness(final ErrorType aType) {
        return aType.equals(ErrorType.BUSINESS);
    }
}
