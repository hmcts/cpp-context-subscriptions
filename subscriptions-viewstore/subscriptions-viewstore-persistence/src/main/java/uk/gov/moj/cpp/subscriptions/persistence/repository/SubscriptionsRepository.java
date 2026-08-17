package uk.gov.moj.cpp.subscriptions.persistence.repository;

import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Subscription view-store repository. Migrated from a DeltaSpike Data {@code @Repository}
 * ({@code EntityRepository<Subscription, UUID>}) to a concrete {@code @ApplicationScoped} JPA repository as
 * part of the Java 25 / Jakarta EE 11 upgrade — DeltaSpike is EOL and unavailable under CDI 4. Each derived /
 * {@code @Query} method becomes an explicit JPQL or native query preserving the original semantics; {@code save}
 * maps to {@code merge} (idempotent under event-listener replay) and {@code findBy(id)} returns the entity (or null).
 */
@ApplicationScoped
public class SubscriptionsRepository {

    @PersistenceContext(unitName = "subscription-persistence-unit")
    EntityManager entityManager;

    public void save(final Subscription subscription) {
        entityManager.merge(subscription);
    }

    public void saveAndFlush(final Subscription subscription) {
        entityManager.merge(subscription);
        entityManager.flush();
    }

    public Subscription findBy(final UUID id) {
        return entityManager.find(Subscription.class, id);
    }

    public void remove(final Subscription subscription) {
        entityManager.remove(entityManager.contains(subscription) ? subscription : entityManager.merge(subscription));
    }

    public List<Subscription> findByOrganisationId(final UUID organisationId) {
        final TypedQuery<Subscription> query = entityManager.createQuery(
                "SELECT s FROM Subscription s WHERE s.organisationId = :organisationId", Subscription.class);
        query.setParameter("organisationId", organisationId);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> findByCourtId(final UUID courtId) {
        return entityManager.createNativeQuery(
                "select s.* from Subscription s where exists(select 1 from court_details c where c.court_id = ?1 " +
                        "and s.id = c.subscription_id)", Subscription.class)
                .setParameter(1, courtId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> findByOrganisationIdAndSubscriber(final UUID organisationId, final String emailAddress) {
        return entityManager.createNativeQuery(
                "select s.* from Subscription s where s.organisation_id = ?1 and exists(select 1 from subscriber sub where sub.email_address = ?2 " +
                        "and s.id = sub.subscription_id)", Subscription.class)
                .setParameter(1, organisationId)
                .setParameter(2, emailAddress)
                .getResultList();
    }
}
