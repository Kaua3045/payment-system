package com.payment.system.application.gateways;

import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;

public interface FraudGateway {

    Assessment assess(Attempt attempt);
}
