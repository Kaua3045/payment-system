package com.payment.system.application.wrapper;

import java.util.function.Supplier;

@FunctionalInterface
public interface TransactionManager {

    <T> T execute(Supplier<T> action);
}

