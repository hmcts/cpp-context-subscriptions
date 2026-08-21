package uk.gov.moj.cpp.subscriptions.persistence.repository;

import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Subscriber view-store repository. Migrated from a DeltaSpike Data {@code @Repository}
 * ({@code EntityRepository<Subscriber, UUID>}) to a concrete {@code @ApplicationScoped} JPA repository as part
 * of the Java 25 / Jakarta EE 11 upgrade. {@code save} maps to {@code merge}; {@code findBy(id)} returns the
 * entity (or null); the {@code @Query} lookup becomes an explicit JPQL query with the same semantics.
 */
@ApplicationScoped
public class SubscribersRepository {

    @PersistenceContext(unitName = "subscription-persistence-unit")
    EntityManager entityManager;

    public void save(final Subscriber subscriber) {
        entityManager.merge(subscriber);
    }

    public Subscriber findBy(final UUID id) {
        return entityManager.find(Subscriber.class, id);
    }

    public Optional<Subscriber> findSubscriberByEmailAddressForSubscription(final String emailAddress, final UUID subscriptionId) {
        final TypedQuery<Subscriber> query = entityManager.createQuery(
                "SELECT subscriber FROM Subscriber subscriber WHERE subscriber.emailAddress = :emailAddress " +
                        "AND subscriber.subscription.id = :subscriptionId", Subscriber.class);
        query.setParameter("emailAddress", emailAddress);
        query.setParameter("subscriptionId", subscriptionId);
        return query.getResultStream().findFirst();
    }
}
