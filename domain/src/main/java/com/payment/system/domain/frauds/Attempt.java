package com.payment.system.domain.frauds;

import com.payment.system.domain.validation.AssertionConcern;
import com.payment.system.domain.valueobjects.Money;

import java.time.Instant;

public record Attempt(
        String id, // Idempotency key used in transaction attempts
        String origin,
        String destination,
        Money amount,
        Instant at,
        GeoPoint location,
        Instant destinationCreatedAt,
        ChannelDetails details,
        RequestContext context
) implements AssertionConcern {

    public Attempt {
        this.assertArgumentNotEmpty(id, "id", "should not be empty");
        this.assertArgumentNotEmpty(origin, "origin", "should not be empty");
        this.assertArgumentNotEmpty(destination, "destination", "should not be empty");
        this.assertArgumentNotNull(amount, "amount", "should not be null");
        this.assertArgumentNotNull(at, "at", "should not be null");
        this.assertArgumentNotNull(details, "details", "should not be null");
        this.assertArgumentNotNull(context, "context", "should not be null");
        // TODO validate non negative in future and amount scale
    }

    public Channel channel() {
        return details.channel();
    }

    public Attempt withLocation(
            final GeoPoint location
    ) {
        return new Attempt(
                this.id(),
                this.origin(),
                this.destination(),
                this.amount(),
                this.at(),
                location,
                this.destinationCreatedAt(),
                this.details(),
                this.context()
        );
    }

    public static Attempt pix(
            final String id,
            final String origin,
            final String destination,
            final String pixKey,
            final Instant pixKeyCreatedAt,
            final Money amount,
            final Instant at,
            final Instant destinationCreatedAt,
            final RequestContext context
    ) {
        return new Attempt(
                id,
                origin,
                destination,
                amount,
                at,
                null,
                destinationCreatedAt,
                new ChannelDetails.Pix(pixKey, pixKeyCreatedAt),
                context
        );
    }
}
