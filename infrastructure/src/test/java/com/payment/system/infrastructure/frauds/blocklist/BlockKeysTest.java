package com.payment.system.infrastructure.frauds.blocklist;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.exceptions.InternalErrorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockKeysTest extends UnitTest {

    @Test
    void shouldAcceptAccountKey() {
        assertEquals("acc:123", BlockKeys.require("acc:123"));
    }

    @Test
    void shouldAcceptPixKey() {
        assertEquals("key:user@example.com",
                BlockKeys.require("key:user@example.com"));
    }

    @Test
    void shouldTrimWhitespace() {
        assertEquals("acc:123", BlockKeys.require("  acc:123  "));
    }

    @Test
    void shouldPreserveColonsInsideValue() {
        assertEquals("key:value:with:colons",
                BlockKeys.require("key:value:with:colons"));
    }

    @Test
    void shouldRejectNull() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require(null)
        );
    }

    @Test
    void shouldRejectEmptyKey() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require("")
        );
    }

    @Test
    void shouldRejectBlankKey() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require("   ")
        );
    }

    @Test
    void shouldRejectMissingColon() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require("acc123")
        );
    }

    @Test
    void shouldRejectEmptyKind() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require(":123")
        );
    }

    @Test
    void shouldRejectEmptyValue() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require("acc:")
        );
    }

    @Test
    void shouldRejectUnsupportedKind() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require("user:123")
        );
    }

    @Test
    void shouldRejectKindWithDifferentCase() {
        assertThrows(
                InternalErrorException.class,
                () -> BlockKeys.require("ACC:123")
        );
    }
}
