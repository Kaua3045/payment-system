package com.payment.system.domain.frauds.model;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigmoidTest extends UnitTest {

    @Test
    void expMatchesMathExp() {
        double worst = 0;
        for (double x = -700; x <= 700; x += 0.37) {
            double ref = Math.exp(x);
            worst = Math.max(worst, Math.abs(Sigmoid.exp(x) - ref) / ref);
        }
        assertTrue(worst < 1e-12, "worst relative error: " + worst);
    }

    @Test
    void knownValues() {
        assertEquals(0.5, Sigmoid.sigmoid(0), 0.0);
        assertEquals(0.8807970779778823, Sigmoid.sigmoid(2), 1e-15);
    }

    @Test
    void isSymmetric() {
        assertEquals(1.0, Sigmoid.sigmoid(3.3) + Sigmoid.sigmoid(-3.3), 1e-15);
    }

    @Test
    void saturatesWithoutOverflow() {
        assertEquals(1.0, Sigmoid.sigmoid(1e9), 0.0);
        assertEquals(0.0, Sigmoid.sigmoid(-1e9), 0.0);
        assertEquals(1.0, Sigmoid.sigmoid(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.0, Sigmoid.sigmoid(Double.NEGATIVE_INFINITY), 0.0);
    }

    @Test
    void nanPropagates() {
        assertTrue(Double.isNaN(Sigmoid.sigmoid(Double.NaN)));
    }

    @Test
    void isMonotonic() {
        for (double z = -40; z < 40; z += 0.01) {
            assertTrue(Sigmoid.sigmoid(z + 0.01) >= Sigmoid.sigmoid(z), "non monotonic in " + z);
        }
    }

    @Test
    void expPropagatesNaN() {
        assertTrue(Double.isNaN(Sigmoid.exp(Double.NaN)));
    }

    @Test
    void expReturnsPositiveInfinityAboveLimit() {
        assertEquals(
                Double.POSITIVE_INFINITY,
                Sigmoid.exp(701.0)
        );
    }
}
