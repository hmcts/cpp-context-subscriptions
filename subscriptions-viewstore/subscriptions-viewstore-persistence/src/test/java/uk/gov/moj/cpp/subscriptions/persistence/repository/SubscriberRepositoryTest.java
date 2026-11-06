package uk.gov.moj.cpp.subscriptions.persistence.repository;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.CHANGE_OF_PLEA;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType.DEFENDANT;
import static uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Event;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Filter;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Real-JPA test for the subscriber/subscription repositories (post DeltaSpike Data → plain JPA migration).
 * Both repositories share the single {@link HibernateTestEntityManagerProvider} EntityManager so writes in one
 * are visible to the other within the per-test transaction (rolled back afterwards). No docker required.
 */
public class SubscriberRepositoryTest {

    private static final String PERSISTENCE_UNIT = "subscriptions-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SubscribersRepository subscribersRepository;

    private SubscriptionsRepository subscriptionsRepository;

    @BeforeEach
    void createRepositoriesWithInjectedEntityManager() {
        subscribersRepository = new SubscribersRepository();
        subscriptionsRepository = new SubscriptionsRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(subscribersRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(subscriptionsRepository);
    }

    @Test
    public void shouldAddSubscribersToTheSubscription() {

        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withActive(true)
                .build();

        final Set<Subscriber> subscribers = new HashSet<>();
        final Subscriber subscriber1 = Subscriber.builder().withId(randomUUID()).withEmailAddress("test@test.com").withActive(true).withSubscription(subscription).build();
        subscribers.add(subscriber1);
        final Filter filter = Filter.builder()
                .withId(randomUUID())
                .withFilterType(DEFENDANT)
                .withDefendantFirstName("First Name")
                .withDefendantLastName("Last Name")
                .withDateOfBirth(parse("1961-03-02"))
                .build();

        final Set<Event> events = new HashSet<>();
        events.add(Event.builder().withId(randomUUID()).withName(CHANGE_OF_PLEA).withSubscription(subscription).build());

        subscription.setEvents(events);
        subscription.setFilter(filter);
        subscription.setSubscribers(subscribers);

        subscriptionsRepository.save(subscription);

        final Subscription persistedSubscription = subscriptionsRepository.findBy(subscription.getId());
        assertThat(persistedSubscription.getId(), is(subscription.getId()));
        assertThat(persistedSubscription.getName(), is(subscription.getName()));
        assertThat(persistedSubscription.isActive(), is(subscription.isActive()));
        assertThat(persistedSubscription.getSubscribers(), hasSize(1));
        assertThat(persistedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber1.getId())).filter(s -> s.isActive()).findFirst().isPresent(), is(true));
        assertThat(persistedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber1.getId())).filter(s -> s.isActive()).findFirst().isPresent(), is(true));
        assertThat(persistedSubscription.getEvents(), hasSize(1));
        assertThat(persistedSubscription.getEvents().stream().anyMatch(e -> e.getName().equals(CHANGE_OF_PLEA)), is(true));


        final Subscriber subscriber2 = Subscriber.builder().withId(randomUUID()).withEmailAddress("test1@test.com").withActive(true).withSubscription(persistedSubscription).build();

        final Set<Subscriber> subscriberSet = persistedSubscription.getSubscribers();
        subscriberSet.add(subscriber2);
        persistedSubscription.setSubscribers(subscriberSet);
        subscribersRepository.save(subscriber2);
        subscriptionsRepository.save(persistedSubscription);

        final Subscription persistedSubscription1 = subscriptionsRepository.findBy(subscription.getId());
        assertThat(persistedSubscription1.getSubscribers(), hasSize(2));

        assertThat(persistedSubscription1.getSubscribers().stream().anyMatch(c -> c.getEmailAddress().equals(subscriber2.getEmailAddress())), is(true));
        assertThat(persistedSubscription1.getSubscribers().stream().filter(s -> s.getId().equals(subscriber2.getId())).filter(s -> s.isActive()).findFirst().isPresent(), is(true));

        final Optional<Subscriber> subscriberByEmailAddressForSubscription = subscribersRepository.findSubscriberByEmailAddressForSubscription("test1@test.com", persistedSubscription.getId());
        assertThat(subscriberByEmailAddressForSubscription.isPresent(), is(true));
        assertThat(subscriberByEmailAddressForSubscription.get().isActive(), is(true));

    }

    @Test
    public void shouldFindSubscriberById() {

        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withActive(true)
                .build();

        final Subscriber subscriber = Subscriber.builder()
                .withId(randomUUID())
                .withEmailAddress("findme@test.com")
                .withActive(true)
                .withSubscription(subscription)
                .build();

        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(subscriber);
        subscription.setSubscribers(subscribers);

        subscriptionsRepository.saveAndFlush(subscription);

        final Subscriber persistedSubscriber = subscribersRepository.findBy(subscriber.getId());
        assertThat(persistedSubscriber.getId(), is(subscriber.getId()));
        assertThat(persistedSubscriber.getEmailAddress(), is("findme@test.com"));
        assertThat(persistedSubscriber.isActive(), is(true));
    }

    @Test
    public void chd01_loadingASubscriberWithANullActiveColumnThrows() {
        final UUID subscriberId = randomUUID();
        insertSubscriberWithNullActive(subscriberId);

        assertThrows(PersistenceException.class, () -> subscribersRepository.findBy(subscriberId));
    }

    @Test
    public void chd06_findSubscriberByEmailAddressForSubscriptionThrowsNoResultExceptionWhenNothingMatches() {
        final Subscription subscription = subscriptionWithSubscribers("someone@test.com");
        subscriptionsRepository.saveAndFlush(subscription);

        assertThrows(NoResultException.class,
                () -> subscribersRepository.findSubscriberByEmailAddressForSubscription("nobody@test.com", subscription.getId()));
    }

    @Test
    public void chd06_findSubscriberByEmailAddressForSubscriptionThrowsNonUniqueResultExceptionWhenSeveralMatch() {
        final Subscription subscription = subscriptionWithSubscribers("twice@test.com", "twice@test.com");
        subscriptionsRepository.saveAndFlush(subscription);

        assertThrows(NonUniqueResultException.class,
                () -> subscribersRepository.findSubscriberByEmailAddressForSubscription("twice@test.com", subscription.getId()));
    }

    private Subscription subscriptionWithSubscribers(final String... emailAddresses) {
        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withActive(true)
                .build();
        final Set<Subscriber> subscribers = new HashSet<>();
        for (final String emailAddress : emailAddresses) {
            subscribers.add(Subscriber.builder().withId(randomUUID()).withEmailAddress(emailAddress).withActive(true).withSubscription(subscription).build());
        }
        subscription.setSubscribers(subscribers);
        return subscription;
    }

    private void insertSubscriberWithNullActive(final UUID subscriberId) {
        final Subscription subscription = subscriptionWithSubscribers();
        subscriptionsRepository.saveAndFlush(subscription);

        final EntityManager entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        entityManager.createNativeQuery("ALTER TABLE subscriber ALTER COLUMN active SET NULL").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO subscriber (id, email_address, subscription_id, active) VALUES (?1, 'legacy@test.com', ?2, NULL)")
                .setParameter(1, subscriberId)
                .setParameter(2, subscription.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

}