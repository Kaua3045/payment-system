package com.payment.system.domain.frauds.rules;

@FunctionalInterface
public interface BlockList {

    boolean contains(String key);
}
