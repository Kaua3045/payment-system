package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.InternalErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBlockListTest extends UnitTest {

    private InMemoryBlockList blockList;

    @BeforeEach
    void setUp() {
        blockList = new InMemoryBlockList();
    }

    @Test
    void shouldReturnFalseWhenKeyIsNotBlocked() {
        assertFalse(blockList.contains("acc:123"));
    }

    @Test
    void shouldBlockAccountKey() {
        blockList.block("acc:123");

        assertTrue(blockList.contains("acc:123"));
    }

    @Test
    void shouldBlockPixKey() {
        blockList.block("key:user@example.com");

        assertTrue(blockList.contains("key:user@example.com"));
    }

    @Test
    void shouldReturnSameInstanceWhenBlockingKey() {
        InMemoryBlockList result = blockList.block("acc:123");

        assertSame(blockList, result);
    }

    @Test
    void shouldNormalizeKeyWhenBlocking() {
        blockList.block("  acc:123  ");

        assertTrue(blockList.contains("acc:123"));
    }

    @Test
    void shouldUnblockExistingKey() {
        blockList.block("acc:123");

        blockList.unblock("acc:123");

        assertFalse(blockList.contains("acc:123"));
    }

    @Test
    void shouldKeepOtherKeysBlockedWhenUnblockingOneKey() {
        blockList.block("acc:123");
        blockList.block("key:user@example.com");

        blockList.unblock("acc:123");

        assertFalse(blockList.contains("acc:123"));
        assertTrue(blockList.contains("key:user@example.com"));
    }

    @Test
    void shouldAllowBlockingSameKeyMoreThanOnce() {
        blockList.block("acc:123");
        blockList.block("acc:123");

        assertTrue(blockList.contains("acc:123"));
    }

    @Test
    void shouldAllowUnblockingKeyThatIsNotBlocked() {
        assertDoesNotThrow(() -> blockList.unblock("acc:123"));

        assertFalse(blockList.contains("acc:123"));
    }

    @Test
    void shouldRejectInvalidKeyWhenBlocking() {
        assertThrows(
                InternalErrorException.class,
                () -> blockList.block("invalid")
        );
    }

    @Test
    void shouldRejectInvalidKeyWhenUnblocking() {
        assertThrows(
                InternalErrorException.class,
                () -> blockList.unblock("invalid")
        );
    }

    @Test
    void shouldNotNormalizeKeyPassedToContains() {
        blockList.block("acc:123");

        assertFalse(blockList.contains(" acc:123 "));
    }
}
