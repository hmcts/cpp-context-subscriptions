package uk.gov.moj.cpp.subscriptions.persistence.repository;

import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType.DEFENDANT;
import static uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Court;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Filter;
import uk.gov.moj.cpp.subscriptions.persistence.entity.NowsEdt;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.Sets;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class SubscriptionsRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private SubscriptionsRepository subscriptionsRepository;

    @Test
    public void shouldSaveTheGivenSubscriptionAndGetById() {

        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withActive(true)
                .build();
        final Set<Court> courts = new HashSet<>();
        final Court courtHouse = Court.builder().withId(randomUUID()).withName("Court House").withSubscription(subscription).build();
        final Court courtHouse1 = Court.builder().withId(randomUUID()).withName("Court House 1").withSubscription(subscription).build();
        courts.add(courtHouse);
        courts.add(courtHouse1);

        subscription.setCourts(courts);
        final Set<NowsEdt> nowsEdts = new HashSet<>();
        final NowsEdt custodialDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Document").build();
        final NowsEdt custodialRemanDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Remand Document").build();

        nowsEdts.add(custodialDocument);
        nowsEdts.add(custodialRemanDocument);

        final Set<Subscriber> subscribers = new HashSet<>();
        final Subscriber subscriber1 = Subscriber.builder().withId(randomUUID()).withEmailAddress("test@test.com").withActive(true).withSubscription(subscription).build();
        final Subscriber subscriber2 = Subscriber.builder().withId(randomUUID()).withEmailAddress("test1@test.com").withActive(true).withSubscription(subscription).build();


        subscribers.add(subscriber1);
        subscribers.add(subscriber2);

        final Filter filter = Filter.builder()
                .withId(randomUUID())
                .withFilterType(DEFENDANT)
                .withDefendantFirstName("First Name")
                .withDefendantLastName("Last Name")
                .withDateOfBirth(parse("1961-03-02"))
                .build();

        subscription.setFilter(filter);
        subscription.setSubscribers(subscribers);
        subscription.setCourts(courts);
        subscription.setNowsEdts(nowsEdts);

        subscriptionsRepository.save(subscription);

        final Subscription persistedSubscription = subscriptionsRepository.findBy(subscription.getId());
        assertThat(persistedSubscription.getId(), is(subscription.getId()));
        assertThat(persistedSubscription.getName(), is(subscription.getName()));
        assertThat(persistedSubscription.isActive(), is(subscription.isActive()));
        assertThat(persistedSubscription.getNowsEdts(), hasSize(2));
        assertThat(persistedSubscription.getNowsEdts().stream().anyMatch(ne -> ne.getId().equals(custodialDocument.getId())), is(true));
        assertThat(persistedSubscription.getNowsEdts().stream().anyMatch(ne -> ne.getName().equals(custodialDocument.getName())), is(true));

        assertThat(persistedSubscription.getNowsEdts().stream().anyMatch(ne -> ne.getId().equals(custodialRemanDocument.getId())), is(true));
        assertThat(persistedSubscription.getNowsEdts().stream().anyMatch(ne -> ne.getName().equals(custodialRemanDocument.getName())), is(true));

        assertThat(persistedSubscription.getCourts().stream().anyMatch(c -> c.getId().equals(courtHouse.getId())), is(true));
        assertThat(persistedSubscription.getCourts().stream().anyMatch(c -> c.getName().equals(courtHouse.getName())), is(true));

        assertThat(persistedSubscription.getCourts().stream().anyMatch(c -> c.getId().equals(courtHouse1.getId())), is(true));
        assertThat(persistedSubscription.getCourts().stream().anyMatch(c -> c.getName().equals(courtHouse1.getName())), is(true));


        assertThat(persistedSubscription.getSubscribers().stream().anyMatch(c -> c.getEmailAddress().equals(subscriber1.getEmailAddress())), is(true));
        assertThat(persistedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber1.getId())).anyMatch(Subscriber::isActive), is(true));

        assertThat(persistedSubscription.getSubscribers().stream().anyMatch(c -> c.getEmailAddress().equals(subscriber2.getEmailAddress())), is(true));
        assertThat(persistedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber2.getId())).anyMatch(Subscriber::isActive), is(true));

        assertThat(persistedSubscription.getFilter().getFilterType(), is(DEFENDANT));

    }

    @Test
    public void shouldFindByOrganisationId() {

        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withOrganisationId(randomUUID())
                .withActive(true)
                .build();

        final Set<NowsEdt> nowsEdts = new HashSet<>();
        final NowsEdt custodialDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Document").withSubscription(subscription).build();
        final NowsEdt custodialRemandDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Remand Document").withSubscription(subscription).build();

        nowsEdts.add(custodialDocument);
        nowsEdts.add(custodialRemandDocument);
        subscription.setNowsEdts(nowsEdts);


        final Subscription subscriptionInAnotherOrganisation = builder()
                .withId(randomUUID())
                .withName("Another Subscription Name")
                .withOrganisationId(randomUUID())
                .withActive(true)
                .build();

        subscriptionsRepository.save(subscription);
        subscriptionsRepository.save(subscriptionInAnotherOrganisation);

        final List<Subscription> persistedSubscriptions = subscriptionsRepository.findByOrganisationId(subscription.getOrganisationId());
        assertThat(persistedSubscriptions, hasSize(1));
        assertThat(persistedSubscriptions.get(0).getId(), is(subscription.getId()));
        assertThat(persistedSubscriptions.get(0).getName(), is(subscription.getName()));
        assertThat(persistedSubscriptions.get(0).getNowsEdts(), hasSize(2));
    }

    @Test
    public void shouldFindByCourtId() {

        final UUID courtId = randomUUID();
        final Court court = Court.builder().withId(randomUUID()).withCourtId(courtId).withName("Wimbledon").build();
        final Court court2 = Court.builder().withId(randomUUID()).withCourtId(courtId).withName("Wimbledon").build();
        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withOrganisationId(randomUUID())
                .withCourts(Sets.newHashSet(court))
                .withActive(true)
                .build();
        final Subscription subscription2 = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withOrganisationId(randomUUID())
                .withCourts(Sets.newHashSet(court2))
                .withActive(true)
                .build();

        final Set<NowsEdt> nowsEdts = new HashSet<>();
        final NowsEdt custodialDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Document").build();
        final NowsEdt custodialRemandDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Remand Document").build();

        nowsEdts.add(custodialDocument);
        nowsEdts.add(custodialRemandDocument);

        custodialDocument.setSubscription(subscription);
        custodialRemandDocument.setSubscription(subscription);
        court.setSubscription(subscription);
        subscription.setNowsEdts(nowsEdts);
        court2.setSubscription(subscription2);
        subscriptionsRepository.saveAndFlush(subscription);
        subscriptionsRepository.saveAndFlush(subscription2);


        final List<Subscription> persistedSubscriptions = subscriptionsRepository.findByCourtId(courtId);
        assertThat(persistedSubscriptions, hasSize(2));
        assertThat(persistedSubscriptions.get(0).getId(), is(subscription.getId()));
        assertThat(persistedSubscriptions.get(0).getName(), is(subscription.getName()));

        assertThat(persistedSubscriptions.get(0).getNowsEdts(), hasSize(2));
        assertThat(persistedSubscriptions.get(0).getNowsEdts().stream().anyMatch(ne -> ne.getId().equals(custodialDocument.getId())), is(true));
        assertThat(persistedSubscriptions.get(0).getNowsEdts().stream().anyMatch(ne -> ne.getName().equals(custodialDocument.getName())), is(true));


    }

    @Test
    public void shouldFindByOrganisationIdAndSubscriber() {

        final Subscription subscription = builder()
                .withId(randomUUID())
                .withName("Subscription Name")
                .withOrganisationId(randomUUID())
                .withActive(true)
                .build();

        final Set<NowsEdt> nowsEdts = new HashSet<>();
        final NowsEdt custodialDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Document").withSubscription(subscription).build();
        final NowsEdt custodialRemandDocument = NowsEdt.builder().withId(randomUUID()).withName("Custodial Remand Document").withSubscription(subscription).build();

        nowsEdts.add(custodialDocument);
        nowsEdts.add(custodialRemandDocument);
        subscription.setNowsEdts(nowsEdts);

        final Set<Subscriber> subscribers = new HashSet<>();
        final Subscriber subscriber1 = Subscriber.builder().withId(randomUUID()).withEmailAddress("test@test.com").withActive(true).withSubscription(subscription).build();
        final Subscriber subscriber2 = Subscriber.builder().withId(randomUUID()).withEmailAddress("test1@test.com").withActive(true).withSubscription(subscription).build();


        subscribers.add(subscriber1);
        subscribers.add(subscriber2);

        final Subscription subscriptionInAnotherOrganisation = builder()
                .withId(randomUUID())
                .withName("Another Subscription Name")
                .withOrganisationId(randomUUID())
                .withSubscribers(subscribers)
                .withActive(true)
                .build();

        subscriptionsRepository.save(subscription);
        subscriptionsRepository.save(subscriptionInAnotherOrganisation);

        final List<Subscription> persistedSubscriptions = subscriptionsRepository.findByOrganisationIdAndSubscriber(subscription.getOrganisationId(), "test1@test.com");
        assertThat(persistedSubscriptions, hasSize(1));
        assertThat(persistedSubscriptions.get(0).getId(), is(subscription.getId()));
        assertThat(persistedSubscriptions.get(0).getName(), is(subscription.getName()));
        assertThat(persistedSubscriptions.get(0).getNowsEdts(), hasSize(2));
    }

}
