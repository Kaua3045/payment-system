package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.domain.frauds.rules.BlockList;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RefreshingBlockList implements BlockList, AutoCloseable {

    private final BlockListSource source;

    private final AtomicReference<Set<String>> current = new AtomicReference<>(Set.of());
    private final ScheduledExecutorService scheduler;
    private final Consumer<RuntimeException> onRefreshError;

    public RefreshingBlockList(BlockListSource source, Duration every) {
        this(source, every, e -> { });
    }

    public RefreshingBlockList(BlockListSource source, Duration every, Consumer<RuntimeException> onRefreshError) {
        this.source = source;
        this.onRefreshError = onRefreshError;
        refresh();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {  // TODO improve this to use virtual threads?
            Thread t = new Thread(r, "blocklist-refresh");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::refreshQuietly, every.toMillis(), every.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean contains(final String key) {
        return this.current.get().contains(key);
    }

    public void refresh() {
        this.current.set(Set.copyOf(source.load()));
    }

    private void refreshQuietly() {
        try {
            refresh();
        } catch (RuntimeException ex) {
            onRefreshError.accept(ex);
        }
    }

    @Override
    public void close() throws Exception {
        scheduler.shutdown();
    }
}
