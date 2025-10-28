package uk.gov.moj.cpp.subscriptions.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.getValueOfField;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleteFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionAggregateTest {

    public static final String EMAIL = "test@test.com";
    @InjectMocks
    private SubscriptionAggregate subscriptionAggregate;

    @Test
    public void shouldProcessSubscriptionByUser() {
        final Subscription subscription = subscription().withId(randomUUID()).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscriptionByUser(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreatedByUser.class));
        SubscriptionCreatedByUser subscriptionCreated = (SubscriptionCreatedByUser) eventList.get(0);
        assertThat(subscriptionCreated.getSubscription().getId(), is(subscription.getId()));
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(getValueOfField(subscriptionAggregate, "organisationId", UUID.class), is(organisationId));
    }


    @Test
    public void shouldProcessSubscription() {
        final Subscription subscription = subscription().withId(randomUUID()).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getSubscription().getId(), is(subscription.getId()));
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(getValueOfField(subscriptionAggregate, "organisationId", UUID.class), is(organisationId));
    }



    @Test
    public void shouldProcessActivateSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withActive(false).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withActive(false).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(false).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(getValueOfField(subscriptionAggregate, "organisationId", UUID.class), is(organisationId));
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(2));
        assertThat(subscriptionAggregate.getSubscribers().get(0).getActive(), is(false));
        assertThat(subscriptionAggregate.getSubscribers().get(1).getActive(), is(false));
        assertThat(subscriptionAggregate.getActive(), is(false));

        //When
        final Stream<Object> activateEventStream = subscriptionAggregate.activateSubscription(organisationId, subscription.getId());

        //Then
        final List<?> activateEventList = activateEventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionActivated.class));
        assertThat(subscriptionAggregate.getSubscribers().get(0).getActive(), is(true));
        assertThat(subscriptionAggregate.getSubscribers().get(1).getActive(), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));

    }

    @Test
    public void shouldProcessDeActivateSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(getValueOfField(subscriptionAggregate, "organisationId", UUID.class), is(organisationId));
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(2));
        assertThat(subscriptionAggregate.getSubscribers().get(0).getActive(), is(true));
        assertThat(subscriptionAggregate.getSubscribers().get(1).getActive(), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));

        //When
        final Stream<Object> activateEventStream = subscriptionAggregate.deactivateSubscription(organisationId, subscription.getId());

        //Then
        final List<?> activateEventList = activateEventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionDeactivated.class));
        assertThat(subscriptionAggregate.getSubscribers().get(0).getActive(), is(false));
        assertThat(subscriptionAggregate.getSubscribers().get(1).getActive(), is(false));
        assertThat(subscriptionAggregate.getActive(), is(false));

    }


    @Test
    public void shouldProcessDeleteSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));

        //When
        final Stream<Object> deleteEventStream = subscriptionAggregate.deleteSubscription(organisationId, subscription.getId());

        //Then
        final List<?> activateEventList = deleteEventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionDeleted.class));
        assertThat(subscriptionAggregate.isDeleted(), is(true));
    }

    @Test
    public void shouldProcessNotDeActivateAfterDeleteSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));

        //When
        final Stream<Object> deleteEventStream = subscriptionAggregate.deleteSubscription(organisationId, subscription.getId());

        //Then
        final List<?> activateEventList = deleteEventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionDeleted.class));
        assertThat(subscriptionAggregate.isDeleted(), is(true));

        final Stream<Object> deactivateEventStream = subscriptionAggregate.deactivateSubscription(organisationId, subscription.getId());
        final List<?> deactivateList = deactivateEventStream.collect(toList());
        assertThat(deactivateList, empty());
    }

    @Test
    public void shouldProcessNotActivateAfterDeleteSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));

        //When
        final Stream<Object> deleteEventStream = subscriptionAggregate.deleteSubscription(organisationId, subscription.getId());

        //Then
        final List<?> activateEventList = deleteEventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionDeleted.class));
        assertThat(subscriptionAggregate.isDeleted(), is(true));

        final Stream<Object> deactivateEventStream = subscriptionAggregate.activateSubscription(organisationId, subscription.getId());
        final List<?> deactivateList = deactivateEventStream.collect(toList());
        assertThat(deactivateList, empty());
    }

    @Test
    public void shouldProcessSubscribe() {

        final Subscription subscription = subscription()
                .withId(randomUUID())
                .withSubscribers(asList(subscribers().withId(randomUUID()).withActive(false).withEmailAddress("test@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(true).withEmailAddress("test1@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(false).withEmailAddress("test2@test.com").build()))
                .withActive(false)
                .build();
        final UUID organisationId = randomUUID();
        subscriptionAggregate.createSubscription(organisationId, subscription);
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(3));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test2@test.com")).noneMatch(s -> s.getActive()), is(true));

        final Stream<Object> eventStream = subscriptionAggregate.subscribe(subscription.getId(), organisationId, "test@test.com");
        final List<?> activateEventList = eventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionSubscribed.class));

        //Then
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test@test.com")).anyMatch(s -> s.getActive()), is(true));

        //And
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test2@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));
    }


    @Test
    public void shouldProcessUnSubscribeLastSubscriberThenSubscriptionIsAlsoDeactivate() {

        final Subscription subscription = subscription()
                .withId(randomUUID())
                .withSubscribers(asList(subscribers().withId(randomUUID()).withActive(false).withEmailAddress("test@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(true).withEmailAddress("test1@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(false).withEmailAddress("test2@test.com").build()))
                .withActive(true)
                .build();
        final UUID organisationId = randomUUID();
        subscriptionAggregate.createSubscription(organisationId, subscription);
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(3));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test2@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));


        final Stream<Object> eventStream = subscriptionAggregate.unsubscribe(subscription.getId(), organisationId, "test1@test.com");
        final List<?> activateEventList = eventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionUnsubscribed.class));

        //Then
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test@test.com")).noneMatch(s -> s.getActive()), is(true));

        //And
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test2@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(false));
    }

    @Test
    public void shouldProcessUnSubscribeOnlySubscriberThenSubscriptionIsAlsoDeactivate() {

        final Subscription subscription = subscription()
                .withId(randomUUID())
                .withSubscribers(asList(subscribers().withId(randomUUID()).withActive(true).withEmailAddress("test1@test.com").build()))
                .withActive(true)
                .build();
        final UUID organisationId = randomUUID();
        subscriptionAggregate.createSubscription(organisationId, subscription);
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(1));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));


        final Stream<Object> eventStream = subscriptionAggregate.unsubscribe(subscription.getId(), organisationId, "test1@test.com");
        final List<?> activateEventList = eventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionUnsubscribed.class));

        //Then

        //And
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(false));
    }


    @Test
    public void shouldProcessSubscribeOnlySubscriberThenSubscriptionIsAlsoActivate() {

        final Subscription subscription = subscription()
                .withId(randomUUID())
                .withSubscribers(asList(subscribers().withId(randomUUID()).withActive(false).withEmailAddress("test1@test.com").build()))
                .withActive(false)
                .build();
        final UUID organisationId = randomUUID();
        subscriptionAggregate.createSubscription(organisationId, subscription);
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(1));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(false));


        final Stream<Object> eventStream = subscriptionAggregate.subscribe(subscription.getId(), organisationId, "test1@test.com");
        final List<?> activateEventList = eventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionSubscribed.class));

        //Then
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("test1@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));
    }

    @Test
    public void shouldDeactivateSubscriptionWithOnlyOneSubscriberWhenUnsubscribeThatSubscriber() {

        final Subscription subscription = subscription()
                .withId(randomUUID())
                .withSubscribers(asList(subscribers().withId(randomUUID()).withActive(true).withEmailAddress("a@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(false).withEmailAddress("b@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(true).withEmailAddress("c@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(true).withEmailAddress("d@test.com").build(),
                        subscribers().withId(randomUUID()).withActive(false).withEmailAddress("e@test.com").build()))
                .withActive(true)
                .build();
        final UUID organisationId = randomUUID();
        subscriptionAggregate.createSubscription(organisationId, subscription);
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(5));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("a@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("b@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("c@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("d@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("e@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));


        final Stream<Object> eventStream = subscriptionAggregate.unsubscribe(subscription.getId(), organisationId, "d@test.com");
        final List<?> activateEventList = eventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionUnsubscribed.class));

        //Then
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("a@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("b@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("c@test.com")).anyMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("d@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getSubscribers().stream().filter(s -> s.getEmailAddress().equals("e@test.com")).noneMatch(s -> s.getActive()), is(true));
        assertThat(subscriptionAggregate.getActive(), is(true));
    }

    @Test
    public void shouldEmitFailedPrivateMessageWhenSubscriptionIsDeleted() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));

        //When
        final Stream<Object> deleteEventStream = subscriptionAggregate.deleteSubscription(organisationId, subscription.getId());

        //Then
        final List<?> activateEventList = deleteEventStream.collect(toList());
        assertThat(activateEventList.get(0), instanceOf(SubscriptionDeleted.class));
        assertThat(subscriptionAggregate.isDeleted(), is(true));

        final Stream<Object> deleteSubscriber = subscriptionAggregate.deleteSubscriber(organisationId, subscription.getId(), EMAIL);
        final List<?> deactivateList = deleteSubscriber.collect(toList());
        assertThat(deactivateList.get(0), instanceOf(SubscriberDeleteFailed.class));
    }

    @Test
    public void shouldEmitFailedPrivateMessageWhenSubscriberNotInSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));

        final Stream<Object> deleteSubscriber = subscriptionAggregate.deleteSubscriber(organisationId, subscription.getId(), "test1@test.com");
        final List<?> deactivateList = deleteSubscriber.collect(toList());
        assertThat(deactivateList.get(0), instanceOf(SubscriberDeleteFailed.class));
    }

    @Test
    public void shouldRaiseSubscriberDeletePrivateMessageForGivenSubscriberWithOtherSubscribersActive() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withEmailAddress("test2@test.com").withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(2));


        final Stream<Object> deleteSubscriber = subscriptionAggregate.deleteSubscriber(organisationId, subscription.getId(), EMAIL);
        final List<?> deactivateList = deleteSubscriber.collect(toList());
        assertThat(deactivateList.get(0), instanceOf(SubscriberDeleted.class));

        assertThat(subscriptionAggregate.getSubscribers(), hasSize(1));
        assertThat(subscriptionAggregate.getSubscribers().stream().noneMatch(s -> s.getEmailAddress().equals(EMAIL)), is(true));
    }


    @Test
    public void shouldRaiseSubscriberDeletedAndSubscriptionDeletedPrivateMessagesWhenOnlyOneSubscriberInSubscription() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.isDeleted(), is(false));
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(1));


        final Stream<Object> deleteSubscriber = subscriptionAggregate.deleteSubscriber(organisationId, subscription.getId(), EMAIL);
        final List<?> deactivateList = deleteSubscriber.collect(toList());
        assertThat(deactivateList.get(0), instanceOf(SubscriberDeleted.class));
        assertThat(deactivateList.get(1), instanceOf(SubscriptionDeleted.class));

        assertThat(subscriptionAggregate.getSubscribers(), empty());
        assertThat(subscriptionAggregate.isDeleted(), is(true));
    }

    @Test
    public void shouldRaiseSubscriberDeletedAndSubscriptionDeactivatedPrivateMessagedWhenOtherSubscribersInSubscriptionAreInactive() {
        //Given
        final Subscribers subscribers1 = subscribers().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Subscribers subscribers2 = subscribers().withId(randomUUID()).withEmailAddress("test1").withActive(false).build();
        final Subscription subscription = subscription().withId(randomUUID()).withActive(true).withSubscribers(asList(subscribers1, subscribers2)).build();
        final UUID organisationId = randomUUID();
        final Stream<Object> eventStream = subscriptionAggregate.createSubscription(organisationId, subscription);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SubscriptionCreated.class));
        SubscriptionCreated subscriptionCreated = (SubscriptionCreated) eventList.get(0);
        assertThat(subscriptionCreated.getOrganisationId(), is(organisationId));
        assertThat(subscriptionAggregate.getId(), is(subscription.getId()));
        assertThat(subscriptionAggregate.getActive(), is(true));
        assertThat(subscriptionAggregate.getSubscribers(), hasSize(2));


        final Stream<Object> deleteSubscriber = subscriptionAggregate.deleteSubscriber(organisationId, subscription.getId(), EMAIL);
        final List<?> deactivateList = deleteSubscriber.collect(toList());
        assertThat(deactivateList.get(0), instanceOf(SubscriberDeleted.class));
        assertThat(deactivateList.get(1), instanceOf(SubscriptionDeactivated.class));

        assertThat(subscriptionAggregate.getSubscribers(), hasSize(1));
        assertThat(subscriptionAggregate.getActive(), is(false));
    }

}
