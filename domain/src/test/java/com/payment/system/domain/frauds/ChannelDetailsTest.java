package com.payment.system.domain.frauds;

import com.payment.system.domain.UnitTest;
import com.payment.system.domain.utils.InstantUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChannelDetailsTest extends UnitTest {

    @Test
    void givenAnValidValues_whenCreatePixChannelDetails_thenShouldBeOk() {
        final var aDetails = new ChannelDetails.Pix("123236781286736812", InstantUtils.now());

        Assertions.assertEquals("123236781286736812", aDetails.key());
        Assertions.assertNotNull(aDetails.instrumentCreatedAt());
        Assertions.assertEquals("key:123236781286736812", aDetails.blockKey());
    }
}
