package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.domain.frauds.rules.BlockList;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// TODO precisa carregar as chaves para ca, via file source, ou statica mesmo
public final class InMemoryBlockList implements BlockList {

    private final Set<String> keys = ConcurrentHashMap.newKeySet();

    public InMemoryBlockList block(String key) {
        keys.add(BlockKeys.require(key));
        return this;
    }

    public void unblock(String key) {
        keys.remove(BlockKeys.require(key));
    }

    @Override
    public boolean contains(String key) {
        return this.keys.contains(key);
    }
}
