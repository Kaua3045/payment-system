package com.kaua.template.domain;

import com.kaua.template.domain.events.DomainEvent;
import com.kaua.template.domain.events.DomainEventPublisher;
import com.kaua.template.domain.utils.IdentifierUtils;
import com.kaua.template.domain.utils.InstantUtils;
import com.kaua.template.domain.validation.ValidationHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

public class EntityTest extends UnitTest {

    @Test
    void testValidEntityCreation() {
        SampleIdentifier sampleId = new SampleIdentifier(UUID.randomUUID().toString());
        Entity<SampleIdentifier> entity = createEntity(sampleId);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals(sampleId, entity.getId());
    }

    @Test
    void testEntityEqualityAndHashCode() {
        final var uuid1 = UUID.randomUUID().toString();
        final var uuid2 = UUID.randomUUID().toString();
        SampleIdentifier id1 = new SampleIdentifier(uuid1);
        SampleIdentifier id2 = new SampleIdentifier(uuid1); // Same as id1
        SampleIdentifier id3 = new SampleIdentifier(uuid2);

        Entity<SampleIdentifier> entity1 = createEntity(id1);

        Entity<SampleIdentifier> entity2 = createEntity(id2);

        Entity<SampleIdentifier> entity3 = createEntity(id3);

        Assertions.assertEquals(entity1.getClass(), entity2.getClass());
        Assertions.assertEquals(entity1, entity1);
        Assertions.assertNotEquals(entity1, entity3);
        Assertions.assertEquals(entity1.hashCode(), entity2.hashCode());
        Assertions.assertNotEquals(entity1.hashCode(), entity3.hashCode());
        Assertions.assertFalse(entity1.equals(null));
        Assertions.assertFalse(entity1.equals(new Object()));
    }

    @Test
    void testInvalidEntityCreation() {
        SampleIdentifier sampleId = new SampleIdentifier(UUID.randomUUID().toString());
        Entity<SampleIdentifier> entity = createEntity(sampleId);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals(sampleId, entity.getId());
        Assertions.assertNotEquals(null, entity);
        Assertions.assertNotEquals(entity, new Object());
    }

    @Test
    void testEntityRegisterEventAndPublishDomainEvent() {
        final var uuid1 = UUID.randomUUID().toString();
        SampleIdentifier id1 = new SampleIdentifier(uuid1);

        Entity<SampleIdentifier> entity1 = createEntity(id1);

        entity1.registerEvent(new SampleEntityEvent(uuid1));

        Assertions.assertEquals(1, entity1.getDomainEvents().size());

        entity1.publishDomainEvents(new SampleEntityPublisherEvent());

        Assertions.assertEquals(0, entity1.getDomainEvents().size());
    }

    @Test
    void testEntityInvalidRegisterEventAndInvalidPublishDomainEvent() {
        final var uuid1 = UUID.randomUUID().toString();
        SampleIdentifier id1 = new SampleIdentifier(uuid1);

        Entity<SampleIdentifier> entity1 = createEntity(id1);

        entity1.registerEvent(null);

        Assertions.assertEquals(0, entity1.getDomainEvents().size());

        entity1.publishDomainEvents(null);

        Assertions.assertEquals(0, entity1.getDomainEvents().size());
    }

    @Test
    void testCreateAggregateRootWithId() {
        SampleIdentifier sampleId = new SampleIdentifier(UUID.randomUUID().toString());
        AggregateRoot<SampleIdentifier> aggregateRoot = new AggregateRoot<>(sampleId) {
            @Override
            public void validate(ValidationHandler aHandler) {

            }
        };

        Assertions.assertNotNull(aggregateRoot);
        Assertions.assertEquals(sampleId, aggregateRoot.getId());
    }

    @Test
    void testCreateAggregateRootWithIdAndVersion() {
        SampleIdentifier sampleId = new SampleIdentifier(UUID.randomUUID().toString());
        AggregateRoot<SampleIdentifier> aggregateRoot = new AggregateRoot<>(sampleId, 1) {
            @Override
            public void validate(ValidationHandler aHandler) {

            }
        };

        Assertions.assertNotNull(aggregateRoot);
        Assertions.assertEquals(sampleId, aggregateRoot.getId());
        Assertions.assertEquals(1, aggregateRoot.getVersion());
    }

    @Test
    void testCreateAggregateRootWithIdVersionAndEvents() {
        SampleIdentifier sampleId = new SampleIdentifier(UUID.randomUUID().toString());
        AggregateRoot<SampleIdentifier> aggregateRoot = new AggregateRoot<>(sampleId, 1, Collections.emptyList()) {
            @Override
            public void validate(ValidationHandler aHandler) {

            }
        };

        Assertions.assertNotNull(aggregateRoot);
        Assertions.assertEquals(sampleId, aggregateRoot.getId());
        Assertions.assertEquals(1, aggregateRoot.getVersion());
    }

    @Test
    void testEntityIncrementVersion() {
        SampleIdentifier sampleId = new SampleIdentifier(UUID.randomUUID().toString());
        Entity<SampleIdentifier> entity = createEntity(sampleId);

        Assertions.assertEquals(0, entity.getVersion());
        entity.incrementVersion();
        Assertions.assertEquals(1, entity.getVersion());
    }

    private Entity<SampleIdentifier> createEntity(SampleIdentifier id) {
        return new Entity<>(id, 0, Collections.emptyList()) {
            @Override
            public void validate(ValidationHandler handler) {
                // Lógica de validação simulada
            }
        };
    }

    record SampleIdentifier(String value) implements Identifier<String> {
    }

    private record SampleEntityEvent(
            String id, String eventId, String eventType, Instant occurredOn,
            String aggregateId,
            long aggregateVersion, String source, String traceId) implements DomainEvent {

        public SampleEntityEvent(final String id) {
            this(id, IdentifierUtils.generateNewIdWithoutHyphen(),
                    "SampleEntityEvent",
                    InstantUtils.now(),
                    id,
                    1,
                    "SampleEntityService",
                    IdentifierUtils.generateNewIdWithoutHyphen());
        }
    }

    private static class SampleEntityPublisherEvent implements DomainEventPublisher {

        @Override
        public <T extends DomainEvent> void publish(T event) {
            // Lógica de publicação de evento simulada
        }
    }
}
