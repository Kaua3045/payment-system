package com.kaua.template.domain.utils;

import com.kaua.template.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class IdentifierUtilsTest extends UnitTest {

    @Test
    void testCallIdentifierUtilsGenerateIdWithHyphen() {
        final var id = IdentifierUtils.generateNewId();

        Assertions.assertNotNull(id);
        Assertions.assertTrue(
                id.matches("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"));
    }

    @Test
    void testCallIdentifierUtilsGenerateIdWithoutHyphen() {
        final var id = IdentifierUtils.generateNewIdWithoutHyphen();

        Assertions.assertNotNull(id);
        Assertions.assertTrue(id.matches("[a-z0-9]{32}"));
    }

    @Test
    void testCallIdentifierUtilsGenerateNewUUID() {
        final var id = IdentifierUtils.generateNewUUID();

        Assertions.assertNotNull(id);
        Assertions.assertInstanceOf(UUID.class, id);
    }

    @Test
    void testCallIdentifierUtilsUUIDToBytes() {
        final var id = UUID.randomUUID();
        final var bytes = IdentifierUtils.getUUIDAsBytes(id);

        Assertions.assertNotNull(bytes);
        Assertions.assertEquals(16, bytes.length);
    }

    @Test
    void testCallIdentifierUtilsBytesToUUID() {
        final var id = UUID.randomUUID();
        final var bytes = IdentifierUtils.getUUIDAsBytes(id);
        final var newId = IdentifierUtils.bytesToUUID(bytes);

        Assertions.assertNotNull(newId);
        Assertions.assertEquals(id, newId);
    }

    @Test
    void testCallIdentifierUtilsGenerateNewULID() {
        final var id = IdentifierUtils.generateNewULID();

        Assertions.assertNotNull(id);
        Assertions.assertTrue(id.toString().matches("[0-9A-Z]{26}"));
    }

    @Test
    void testCallIdentifierUtilsGenerateNewMonotonicULID() {
        final var id = IdentifierUtils.generateNewMonotonicULID();

        Assertions.assertNotNull(id);
        Assertions.assertTrue(id.toString().matches("[0-9A-Z]{26}"));
    }
}
