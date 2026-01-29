package com.payment.system.application;

public abstract class UnitUseCase<I> {

    public abstract void execute(I input);
}
