package com.payment.system.application;

public abstract class UseCase<I, O> {

    public abstract O execute(I input);
}
