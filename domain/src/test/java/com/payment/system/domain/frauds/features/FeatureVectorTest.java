package com.payment.system.domain.frauds.features;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FeatureVectorTest extends UnitTest {

    @Test
    void givenAnInvalidLength_whenCallsFeatureVectorOf_shouldThrowException() {
        final var expectedErrorMessage = "requested 12 features, received 1";

        final var aException = Assertions.assertThrows(IllegalArgumentException.class, () -> FeatureVector.of(10));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenAnInvalidNaN_whenCallsFeatureVectorOf_shouldThrowException() {
        final var expectedErrorMessage = "feature invalid (NaN/Infinity): log_amount";

        final var aException = Assertions.assertThrows(IllegalArgumentException.class, () -> FeatureVector.of(Double.NaN, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }

    @Test
    void givenAnInvalidInfinity_whenCallsFeatureVectorOf_shouldThrowException() {
        final var expectedErrorMessage = "feature invalid (NaN/Infinity): log_amount";

        final var aException = Assertions.assertThrows(IllegalArgumentException.class, () -> FeatureVector.of(Double.POSITIVE_INFINITY, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));

        Assertions.assertEquals(expectedErrorMessage, aException.getMessage());
    }
}
