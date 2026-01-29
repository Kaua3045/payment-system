package com.kaua.template.domain.utils;

import com.kaua.template.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.time.Instant;

class ULIDTest extends UnitTest {

    @Test
    void testGetULIDValues() {
        ULID aUlid = ULID.random();

        Assertions.assertNotNull(aUlid);
        Assertions.assertInstanceOf(ULID.class, aUlid);
        Assertions.assertInstanceOf(Long.class, aUlid.getMsb());
        Assertions.assertInstanceOf(Long.class, aUlid.getLsb());
        Assertions.assertInstanceOf(Long.class, aUlid.getTimestamp());
        Assertions.assertNotNull(aUlid.getEntropy());
    }

    @Test
    void testGetULIDValuesAsString() {
        ULID aUlid = ULID.random();

        Assertions.assertNotNull(aUlid);
        Assertions.assertNotNull(aUlid.toString());
    }

    @Test
    void testGetULIDValuesAsBytes() {
        ULID aUlid = ULID.random();

        Assertions.assertNotNull(aUlid);
        Assertions.assertNotNull(aUlid.toBytes());
    }

    @Test
    void testFromStringULID() {
        ULID aUlid = ULID.random();
        String ulidString = aUlid.toString();
        ULID fromString = ULID.fromString(ulidString);

        Assertions.assertNotNull(fromString);
        Assertions.assertEquals(aUlid, fromString);
    }

    @Test
    void testFromStringWithValueLengthInvalid() {
        String invalidValue = "01D3Z1E4X1ZQZQZQZQZQZQZQZ";
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.fromString(invalidValue));
    }

    @Test
    void testFromBytesULID() {
        ULID aUlid = ULID.random();
        byte[] ulidBytes = aUlid.toBytes();
        ULID fromBytes = ULID.fromBytes(ulidBytes);

        Assertions.assertNotNull(fromBytes);
        Assertions.assertEquals(aUlid, fromBytes);
    }

    @Test
    void testFromBytesULIDWithNull() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.fromBytes(null));
    }

    @Test
    void testFromBytesULIDWithInvalidLength() {
        byte[] invalidBytes = new byte[10];
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.fromBytes(invalidBytes));
    }

    @Test
    void testGenerateULIDMethodSuccess() {
        final long aNow = Instant.now().toEpochMilli();
        final byte[] aEntropy = new byte[ULID.ENTROPY_LENGTH];
        new SecureRandom().nextBytes(aEntropy);

        Assertions.assertDoesNotThrow(() -> ULID.generate(aNow, aEntropy));
    }

    @Test
    void testGenerateULIDMethodWithInvalidNullEntropy() {
        final long aNow = Instant.now().toEpochMilli();

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.generate(aNow, null));
    }

    @Test
    void testGenerateULIDMethodWithInvalidEntropyLength() {
        final long aNow = Instant.now().toEpochMilli();
        final byte[] aEntropy = new byte[5];

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.generate(aNow, aEntropy));
    }

    @Test
    void testGenerateULIDMethodWithInvalidMinTime() {
        final long aNow = -1L;
        final byte[] aEntropy = new byte[ULID.ENTROPY_LENGTH];
        new SecureRandom().nextBytes(aEntropy);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.generate(aNow, aEntropy));
    }

    @Test
    void testGenerateULIDMethodWithInvalidMaxTime() {
        final long aNow = ULID.MAX_TIME + 1;
        final byte[] aEntropy = new byte[ULID.ENTROPY_LENGTH];
        new SecureRandom().nextBytes(aEntropy);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ULID.generate(aNow, aEntropy));
    }

    @Test
    void testCompareToULID() {
        ULID ulid1 = new ULID(0x0000000000000001L, 0x0000000000000001L);
        ULID ulid2 = new ULID(0x0000000000000002L, 0x0000000000000001L);
        ULID ulid3 = new ULID(0x0000000000000001L, 0x0000000000000002L);
        ULID ulid4 = new ULID(0x0000000000000001L, 0x0000000000000001L);

        // Basic comparisons
        Assertions.assertTrue(ulid1.compareTo(ulid2) < 0);
        Assertions.assertTrue(ulid2.compareTo(ulid1) > 0);
        Assertions.assertTrue(ulid1.compareTo(ulid3) < 0);
        Assertions.assertTrue(ulid3.compareTo(ulid1) > 0);
        Assertions.assertEquals(0, ulid1.compareTo(ulid4));

        // Reflexivity
        Assertions.assertEquals(0, ulid1.compareTo(ulid1));

        // Transitivity
        Assertions.assertTrue(ulid1.compareTo(ulid3) < 0);
        Assertions.assertTrue(ulid3.compareTo(ulid2) < 0);
        Assertions.assertTrue(ulid1.compareTo(ulid2) < 0);
    }

    @Test
    void testEqualsULID() {
        ULID aUlid = ULID.random();
        ULID anotherUlid = ULID.random();

        Assertions.assertNotEquals(aUlid, anotherUlid);
        Assertions.assertEquals(aUlid, aUlid);
        Assertions.assertNotEquals(aUlid, null);
        Assertions.assertNotEquals(aUlid, new Object());
    }

    @Test
    void testEqualsAndHashCodeULID() {
        ULID ulid1 = new ULID(0x123456789ABCDEFL, 0xFEDCBA9876543210L);
        ULID ulid2 = new ULID(0x123456789ABCDEFL, 0xFEDCBA9876543210L);
        ULID ulid3 = new ULID(0x123456789ABCDEFL, 0x1111111111111111L);

        Assertions.assertEquals(ulid1, ulid2);
        Assertions.assertNotEquals(ulid1, ulid3);
        Assertions.assertEquals(ulid1.hashCode(), ulid2.hashCode());
    }

    @Test
    void testValidateUlidMethodWithInvalidAsciiCharacter() {
        char invalidChar = (char) 127; // Maior que o limite DECODING
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ULID.validateUlid(invalidChar);
        });
        Assertions.assertEquals("Invalid ULID character: " + invalidChar, exception.getMessage());
    }

    @Test
    void testValidateUlidMethodWithUnmappedCharacter() {
        char unmappedChar = '*'; // Qualquer caracter não presente no array DECODING
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ULID.validateUlid(unmappedChar);
        });
        Assertions.assertEquals("Invalid ULID character: " + unmappedChar, exception.getMessage());
    }

    @Test
    void testValidateUlidMethodWithHighAsciiCharacter() {
        char highAsciiChar = '{'; // ASCII 123
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ULID.validateUlid(highAsciiChar);
        });
        Assertions.assertEquals("Invalid ULID character: " + highAsciiChar, exception.getMessage());
    }

    @Test
    void testEncodeBase32UlidMethodWithInvalidParamsThrowsException() throws Exception {
        final var expectedErrorMessage = "Destination array too small, destination length: 25";

        ULID ulid = new ULID(0x123456789ABCDEFL, 0xFEDCBA9876543210L);

        Method method = ULID.class.getDeclaredMethod(
                "encodeBase32",
                long.class,
                int.class,
                char[].class,
                int.class
        ); // private method
        method.setAccessible(true); // set accessible to true

        long aValue = 0x123456789ABCDEFL;
        int aLength = 26;
        char[] aDest = new char[25];
        int aOffset = 0;

        final var aException = Assertions.assertThrows(InvocationTargetException.class, () -> {
            method.invoke(ulid, aValue, aLength, aDest, aOffset);
        }); // invoke the method with invalid length

        final var aCause = aException.getCause();

        Assertions.assertInstanceOf(IllegalArgumentException.class, aCause);
        Assertions.assertEquals(expectedErrorMessage, aCause.getMessage());
    }
}