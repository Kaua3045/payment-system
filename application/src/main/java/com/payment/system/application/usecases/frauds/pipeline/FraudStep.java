package com.payment.system.application.usecases.frauds.pipeline;

public interface FraudStep {

    String name();
    void run(FraudCase fraudCase) throws Exception;
    void onFailure(FraudCase fraudCase, Exception error);
}
