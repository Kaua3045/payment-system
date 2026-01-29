package com.payment.system.domain.events;

@FunctionalInterface
public interface DomainEventPublisher {

    <T extends DomainEvent> void publish(T event);
}
