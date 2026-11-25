package com.payment.system.infrastructure.configurations.properties;

import com.payment.system.infrastructure.frauds.blocklist.BlockListType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.math.BigDecimal;
import java.nio.file.Path;

public class FraudSettings implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FraudSettings.class);

    private String modelPath;
    private double reviewAt;
    private double denyAt;
    private BigDecimal amountLimit;
    private BigDecimal nightLimit;
    private int maxPerMinute;
    private double velocityBoost;
    private double maxSpeedKmh;
    private double travelBoost;
    private BigDecimal failOpenBelow;
    private BlockListType blockListType;
    private int blockListRefreshIntervalSeconds;

    @Override
    public void afterPropertiesSet() {
        log.info("Initialized FraudSettings {}", this);
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public double getReviewAt() {
        return reviewAt;
    }

    public void setReviewAt(double reviewAt) {
        this.reviewAt = reviewAt;
    }

    public double getDenyAt() {
        return denyAt;
    }

    public void setDenyAt(double denyAt) {
        this.denyAt = denyAt;
    }

    public BigDecimal getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    public BigDecimal getNightLimit() {
        return nightLimit;
    }

    public void setNightLimit(BigDecimal nightLimit) {
        this.nightLimit = nightLimit;
    }

    public int getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public double getVelocityBoost() {
        return velocityBoost;
    }

    public void setVelocityBoost(double velocityBoost) {
        this.velocityBoost = velocityBoost;
    }

    public double getMaxSpeedKmh() {
        return maxSpeedKmh;
    }

    public void setMaxSpeedKmh(double maxSpeedKmh) {
        this.maxSpeedKmh = maxSpeedKmh;
    }

    public double getTravelBoost() {
        return travelBoost;
    }

    public void setTravelBoost(double travelBoost) {
        this.travelBoost = travelBoost;
    }

    public BigDecimal getFailOpenBelow() {
        return failOpenBelow;
    }

    public void setFailOpenBelow(BigDecimal failOpenBelow) {
        this.failOpenBelow = failOpenBelow;
    }

    public BlockListType getBlockListType() {
        return blockListType;
    }

    public void setBlockListType(BlockListType blockListType) {
        this.blockListType = blockListType;
    }

    public int getBlockListRefreshIntervalSeconds() {
        return blockListRefreshIntervalSeconds;
    }

    public void setBlockListRefreshIntervalSeconds(int blockListRefreshIntervalSeconds) {
        this.blockListRefreshIntervalSeconds = blockListRefreshIntervalSeconds;
    }

    @Override
    public String toString() {
        return "FraudSettings{" +
                "modelPath=" + modelPath +
                ", reviewAt=" + reviewAt +
                ", denyAt=" + denyAt +
                ", amountLimit=" + amountLimit +
                ", nightLimit=" + nightLimit +
                ", maxPerMinute=" + maxPerMinute +
                ", velocityBoost=" + velocityBoost +
                ", maxSpeedKmh=" + maxSpeedKmh +
                ", travelBoost=" + travelBoost +
                ", failOpenBelow=" + failOpenBelow +
                ", blockListType=" + blockListType +
                ", blockListRefreshIntervalSeconds=" + blockListRefreshIntervalSeconds +
                '}';
    }
}
