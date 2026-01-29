package com.kaua.template.domain.utils;

import com.kaua.template.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InstantUtilsTest extends UnitTest {

    @Test
    void testCallInstantUtilsNow_thenReturnInstant() {
        final var aNow = InstantUtils.now();
        Assertions.assertNotNull(aNow);
    }

    @Test
    void givenInstantString_whenCallInstantUtilsFromString_thenReturnInstant() {
        final var aNow = InstantUtils.now();
        final var aNowString = aNow.toString();
        final var aNowFromString = InstantUtils.fromString(aNowString);

        Assertions.assertTrue(aNowFromString.isPresent());
        Assertions.assertEquals(aNow, aNowFromString.get());
    }

    @Test
    void givenAnNullInstantString_whenCallInstantUtilsFromString_thenReturnEmpty() {
        final var aNowFromString = InstantUtils.fromString(null);

        Assertions.assertTrue(aNowFromString.isEmpty());
    }

    @Test
    void givenAnBlankInstantString_whenCallInstantUtilsFromString_thenReturnEmpty() {
        final var aNowFromString = InstantUtils.fromString("");

        Assertions.assertTrue(aNowFromString.isEmpty());
    }
}
