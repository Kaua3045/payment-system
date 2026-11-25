package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshingBlockListTest extends UnitTest {

    @Test
    void shouldLoadInitialSnapshotOnConstruction() throws Exception {
        BlockListSource source = mock(BlockListSource.class);

        when(source.load()).thenReturn(Set.of("acc:123", "key:abc"));

        try (var blockList = new RefreshingBlockList(source, Duration.ofHours(1))) {
            assertTrue(blockList.contains("acc:123"));
            assertTrue(blockList.contains("key:abc"));
            assertFalse(blockList.contains("card:456"));
        }

        verify(source).load();
    }

    @Test
    void shouldReplaceSnapshotOnRefresh() throws Exception {
        AtomicReference<Set<String>> entries =
                new AtomicReference<>(Set.of("acc:123"));

        BlockListSource source = entries::get;

        try (var blockList = new RefreshingBlockList(source, Duration.ofHours(1))) {
            assertTrue(blockList.contains("acc:123"));

            entries.set(Set.of("acc:456", "key:abc"));

            blockList.refresh();

            assertFalse(blockList.contains("acc:123"));
            assertTrue(blockList.contains("acc:456"));
            assertTrue(blockList.contains("key:abc"));
        }
    }

    @Test
    void shouldPreservePreviousSnapshotWhenRefreshFails() throws Exception {
        AtomicInteger calls = new AtomicInteger();

        BlockListSource source = () -> {
            if (calls.incrementAndGet() == 1) {
                return Set.of("acc:123");
            }

            throw new IllegalStateException("Database unavailable");
        };

        try (var blockList = new RefreshingBlockList(source, Duration.ofHours(1))) {
            assertTrue(blockList.contains("acc:123"));

            assertThrows(
                    IllegalStateException.class,
                    blockList::refresh
            );

            assertTrue(blockList.contains("acc:123"));
            assertFalse(blockList.contains("acc:456"));
        }
    }

    @Test
    void shouldNotifyWhenScheduledRefreshFails() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch errorReceived = new CountDownLatch(1);

        BlockListSource source = () -> {
            if (calls.incrementAndGet() == 1) {
                return Set.of("acc:123");
            }

            throw new IllegalStateException("Database unavailable");
        };

        try (var blockList = new RefreshingBlockList(
                source,
                Duration.ofMillis(10),
                error -> errorReceived.countDown()
        )) {
            assertTrue(blockList.contains("acc:123"));

            assertTrue(errorReceived.await(2, TimeUnit.SECONDS));

            assertTrue(blockList.contains("acc:123"));
        }
    }

    @Test
    void shouldStartWithEmptySnapshotWhenSourceReturnsNoEntries() throws Exception {
        BlockListSource source = Set::of;

        try (var blockList = new RefreshingBlockList(source, Duration.ofHours(1))) {
            assertFalse(blockList.contains("acc:123"));
        }
    }

    @Test
    void shouldLoadBlockListUsingDefaultErrorHandler() throws Exception {
        BlockListSource source = () -> Set.of("acc:123");

        try (var blockList = new RefreshingBlockList(source, Duration.ofHours(1))) {
            assertTrue(blockList.contains("acc:123"));
        }
    }

    @Test
    void shouldRefreshSnapshotSuccessfully() throws InterruptedException {
        AtomicReference<Set<String>> entries =
                new AtomicReference<>(Set.of("acc:123"));

        BlockListSource source = entries::get;

        try (var blockList = new RefreshingBlockList(source, Duration.ofMillis(100))) {
            assertTrue(blockList.contains("acc:123"));

            entries.set(Set.of("acc:456"));

            await()
                    .atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> {
                        assertFalse(blockList.contains("acc:123"));
                        assertTrue(blockList.contains("acc:456"));
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
