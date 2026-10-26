package com.payment.system.application.repositories;

import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Stats;

public interface StatsRepository {

    Stats load(Attempt attempt);
    void record(Attempt attempt, Assessment result);
}
