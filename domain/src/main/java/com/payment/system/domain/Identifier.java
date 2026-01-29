package com.payment.system.domain;

public interface Identifier<T> extends ValueObject {

    T value();
}
