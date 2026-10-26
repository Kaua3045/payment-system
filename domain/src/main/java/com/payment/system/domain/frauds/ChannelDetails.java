package com.payment.system.domain.frauds;

import com.payment.system.domain.validation.AssertionConcern;

import java.time.Instant;

public sealed interface ChannelDetails {

    Channel channel();

    // This method returns a block pix key
    default String blockKey() {
        return null;
    }

    // This method returns the creation timestamp of the instrument associated with the channel, if applicable.
    default Instant instrumentCreatedAt() {
        return null;
    }

    record Pix(String key, Instant keyCreatedAt) implements ChannelDetails, AssertionConcern {
        public Pix {
            this.assertArgumentNotEmpty(key, "key", "should not be empty");
        }

        @Override
        public Channel channel() {
            return Channel.PIX;
        }

        @Override
        public String blockKey() {
            return "key:" + key;
        }

        @Override
        public Instant instrumentCreatedAt() {
            return keyCreatedAt;
        }
    }
}
