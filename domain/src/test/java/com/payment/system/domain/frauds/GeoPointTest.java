package com.payment.system.domain.frauds;

import com.payment.system.domain.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GeoPointTest extends UnitTest {

    @Test
    void givenAnValidGeoPoint_whenCreated_thenShouldBeEqual() {
        final var geoPoint = new GeoPoint(10.0, 20.0, "US");
        final var anotherGeoPoint = new GeoPoint(10.0, 20.0, "US");

        Assertions.assertEquals(geoPoint, anotherGeoPoint);
    }
}
