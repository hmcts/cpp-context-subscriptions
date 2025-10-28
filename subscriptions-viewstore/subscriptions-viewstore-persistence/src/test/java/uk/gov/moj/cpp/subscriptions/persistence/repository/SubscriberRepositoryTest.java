package uk.gov.moj.cpp.subscriptions.persistence.repository;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.CHANGE_OF_PLEA;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType.DEFENDANT;
import static uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Event;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Filter;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class SubscriberRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private SubscribersRepository subscribersRepository;

    @Inject
    private SubscriptionsRepository subscriptionsRepository;


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

        final Subscriber subscriberByEmailAddressForSubscription = subscribersRepository.findSubscriberByEmailAddressForSubscription("test1@test.com", persistedSubscription.getId());
        assertThat(subscriberByEmailAddressForSubscription, notNullValue());
        assertThat(subscriberByEmailAddressForSubscription.isActive(), is(true));

    }

}