package uk.gov.moj.cpp.subscriptions.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted.subscriberDeleted;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed.subscriptionSubscribed;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed.subscriptionUnsubscribed;
import static uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber.builder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;
import uk.gov.moj.cpp.subscriptions.persistence.repository.SubscriptionsRepository;

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriberEventListenerTest {


    public static final String EMAIL = "test@test.com";
    public static final String EMAIL2 = "test2@test.com";
    @InjectMocks
    private SubscriberEventListener subscriberEventListener;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Metadata metadata;

    @Mock
    private SubscriptionsRepository subscriptionsRepository;


    @Captor
    private ArgumentCaptor<Subscription> subscriptionsRepositoryArgumentCaptor;

    @Test
    public void shouldSubscribeSubscription() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionSubscribed subscriptionSubscribed = subscriptionSubscribed().withSubscriptionId(randomUUID()).withSubscriber(EMAIL).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionSubscribed.class)).thenReturn(subscriptionSubscribed);
        final Subscriber subscriber = builder().withId(randomUUID()).withEmailAddress(EMAIL).withActive(false).build();

        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(subscriber);
        subscribers.add(Subscriber.builder().withId(randomUUID()).withEmailAddress(EMAIL2).withActive(false).build());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionSubscribed.getSubscriptionId())
                .withActive(false)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriptionSubscribed.getSubscriptionId())).thenReturn(subscription);

        subscriberEventListener.handleSubscribe(envelope);

        verify(subscriptionsRepository).save(subscriptionsRepositoryArgumentCaptor.capture());
        final Subscription updatedSubscription = subscriptionsRepositoryArgumentCaptor.getValue();
        assertThat(updatedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber.getId())).anyMatch(Subscriber::isActive), is(true));
        assertThat(updatedSubscription.getSubscribers().stream().filter(s -> s.getEmailAddress().equals(EMAIL2)).noneMatch(Subscriber::isActive), is(true));
        assertThat(updatedSubscription.isActive(), is(true));

    }

    @Test
    public void shouldUnsubscribeOnlySubscriberThenDeactivateSubscription() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionUnsubscribed subscriptionUnsubscribed = subscriptionUnsubscribed().withSubscriptionId(randomUUID()).withSubscriber(EMAIL).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionUnsubscribed.class)).thenReturn(subscriptionUnsubscribed);
        final Subscriber subscriber = builder().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(subscriber);
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionUnsubscribed.getSubscriptionId())
                .withActive(true)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriptionUnsubscribed.getSubscriptionId())).thenReturn(subscription);

        subscriberEventListener.handleUnSubscribe(envelope);

        verify(subscriptionsRepository).save(subscriptionsRepositoryArgumentCaptor.capture());
        final Subscription updatedSubscription = subscriptionsRepositoryArgumentCaptor.getValue();
        assertThat(updatedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber.getId())).noneMatch(Subscriber::isActive), is(true));

        assertThat(updatedSubscription.isActive(), is(false));
    }


    @Test
    public void shouldUnsubscribeLastSubscriberThenDeactivateSubscription() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionUnsubscribed subscriptionUnsubscribed = subscriptionUnsubscribed().withSubscriptionId(randomUUID()).withSubscriber(EMAIL).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionUnsubscribed.class)).thenReturn(subscriptionUnsubscribed);
        final Subscriber subscriber = builder().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(subscriber);
        subscribers.add(Subscriber.builder().withId(randomUUID()).withEmailAddress(EMAIL2).withActive(false).build());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionUnsubscribed.getSubscriptionId())
                .withActive(true)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriptionUnsubscribed.getSubscriptionId())).thenReturn(subscription);

        subscriberEventListener.handleUnSubscribe(envelope);

        verify(subscriptionsRepository).save(subscriptionsRepositoryArgumentCaptor.capture());
        final Subscription updatedSubscription = subscriptionsRepositoryArgumentCaptor.getValue();
        assertThat(updatedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber.getId())).noneMatch(Subscriber::isActive), is(true));

        assertThat(updatedSubscription.isActive(), is(false));
    }

    @Test
    public void shouldUnsubscribeAnyOneSubscriberWithOthersActiveThenDoNotDeActivateSubscriptionStatus() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionUnsubscribed subscriptionUnsubscribed = subscriptionUnsubscribed().withSubscriptionId(randomUUID()).withSubscriber(EMAIL).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionUnsubscribed.class)).thenReturn(subscriptionUnsubscribed);
        final Subscriber subscriber = builder().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build();
        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(builder().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build());
        subscribers.add(builder().withId(randomUUID()).withEmailAddress(EMAIL2).withActive(true).build());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionUnsubscribed.getSubscriptionId())
                .withActive(true)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriptionUnsubscribed.getSubscriptionId())).thenReturn(subscription);
        subscriberEventListener.handleUnSubscribe(envelope);
        verify(subscriptionsRepository).save(subscriptionsRepositoryArgumentCaptor.capture());
        final Subscription updatedSubscription = subscriptionsRepositoryArgumentCaptor.getValue();
        assertThat(updatedSubscription.getSubscribers().stream().filter(s -> s.getId().equals(subscriber.getId())).noneMatch(Subscriber::isActive), is(true));

    }

    @Test
    public void shouldDeleteSubscriber() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriberDeleted subscriberDeleted = subscriberDeleted().withSubscriptionId(randomUUID()).withSubscriber(EMAIL).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriberDeleted.class)).thenReturn(subscriberDeleted);
        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(builder().withId(randomUUID()).withEmailAddress(EMAIL).withActive(true).build());
        subscribers.add(builder().withId(randomUUID()).withEmailAddress(EMAIL2).withActive(true).build());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriberDeleted.getSubscriptionId())
                .withActive(true)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriberDeleted.getSubscriptionId())).thenReturn(subscription);
        subscriberEventListener.handleDeleteSubscriber(envelope);
        verify(subscriptionsRepository).save(subscriptionsRepositoryArgumentCaptor.capture());
        final Subscription updatedSubscription = subscriptionsRepositoryArgumentCaptor.getValue();
        assertThat(updatedSubscription.getSubscribers().stream().noneMatch(s -> s.getEmailAddress().equals(EMAIL)), is(true));


    }


}