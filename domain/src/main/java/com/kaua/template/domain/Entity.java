package com.kaua.template.domain;

import com.kaua.template.domain.events.DomainEvent;
import com.kaua.template.domain.events.DomainEventPublisher;
import com.kaua.template.domain.validation.ValidationHandler;

import java.util.*;

public abstract class Entity<ID extends Identifier> {

    private final ID id;
    private long version;
    private final List<DomainEvent> domainEvents;

    protected Entity(final ID id, final long version) {
        this(id, version, null);
    }

    protected Entity(final ID id, final long version, final List<DomainEvent> domainEvents) {
        this.id = Objects.requireNonNull(id, "'id' should not be null");
        this.version = version;
        this.domainEvents = new ArrayList<>(domainEvents == null ? Collections.emptyList() : domainEvents);
    }

    public abstract void validate(ValidationHandler aHandler);

    public ID getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version++;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void publishDomainEvents(final DomainEventPublisher publisher) {
        if (publisher == null) {
            return;
        }

        getDomainEvents().forEach(publisher::publish);

        this.domainEvents.clear();
    }

    public void registerEvent(final DomainEvent event) {
        if (event == null) {
            return;
        }

        this.domainEvents.add(event);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Entity<?> entity = (Entity<?>) o;
        return Objects.equals(getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
