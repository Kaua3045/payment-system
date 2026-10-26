package com.payment.system.application.usecases.frauds.pipeline;

import com.payment.system.domain.exceptions.InternalErrorException;
import com.payment.system.domain.frauds.Assessment;
import com.payment.system.domain.frauds.Attempt;
import com.payment.system.domain.frauds.Stats;
import com.payment.system.domain.frauds.Verdict;
import com.payment.system.domain.frauds.features.FeatureVector;

import java.util.ArrayList;
import java.util.List;

public final class FraudCase {

    final Attempt attempt;
    Stats stats;
    FeatureVector features;
    double boostLogit;
    double score = Double.NaN;
    Verdict verdict;
    boolean degraded;
    final List<String> reasons = new ArrayList<>(4);
    final List<String> path = new ArrayList<>(4);

    FraudCase(final Attempt attempt) {
        this.attempt = attempt;
    }

    boolean isDecided() {
        return verdict != null;
    }

    public void decide(final Verdict verdict, final double score) {
        this.verdict = verdict;
        this.score = score;
    }

    public Assessment toAssessment() {
        if (verdict == null) {
            throw InternalErrorException.with("No stage reached a decision; the pipeline was poorly set up");
        }
        return new Assessment(verdict, score, List.copyOf(reasons), degraded);
    }

    public String path() {
        return String.join(">", path);
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public void addReason(final String reason) {
        this.reasons.add(reason);
    }

    public void boostLogitIncrement(double boostLogit) {
        this.boostLogit += boostLogit;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public FeatureVector getFeatures() {
        return features;
    }

    public void setFeatures(FeatureVector features) {
        this.features = features;
    }

    public double getBoostLogit() {
        return boostLogit;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
