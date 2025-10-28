package uk.gov.moj.cpp.subscriptions.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;
import uk.gov.moj.cpp.subscriptions.persistence.repository.SubscriptionsRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SubscriberEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private SubscriptionsRepository subscriptionsRepository;


    @Handles("subscriptions.event.subscription-subscribed")
    public void handleSubscribe(final JsonEnvelope event) {
        final SubscriptionSubscribed subscriptionSubscribed = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionSubscribed.class);
        processSubscribeUnsubscribe(subscriptionSubscribed.getSubscriber(), subscriptionSubscribed.getSubscriptionId(), true);
    }


    @Handles("subscriptions.event.subscription-unsubscribed")
    public void handleUnSubscribe(final JsonEnvelope event) {
        final SubscriptionUnsubscribed subscriptionUnsubscribed = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionUnsubscribed.class);
        processSubscribeUnsubscribe(subscriptionUnsubscribed.getSubscriber(), subscriptionUnsubscribed.getSubscriptionId(), false);
    }

    @Handles("subscriptions.event.subscriber-deleted")
    public void handleDeleteSubscriber(final JsonEnvelope event) {
        final SubscriberDeleted subscriberDeleted = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriberDeleted.class);
        final Subscription subscription = subscriptionsRepository.findBy(subscriberDeleted.getSubscriptionId());
        final Optional<Subscriber> optionalSubscriber = subscription.getSubscribers().stream().filter(s -> s.getEmailAddress().equals(subscriberDeleted.getSubscriber())).findFirst();
        if(optionalSubscriber.isPresent()) {
            subscription.getSubscribers().remove(optionalSubscriber.get());
            subscriptionsRepository.save(subscription);
        }
    }


    private void processSubscribeUnsubscribe(final String subscriber, final UUID subscriptionId, final boolean isSubscribed) {
        final Subscription subscription = subscriptionsRepository.findBy(subscriptionId);
        subscription.getSubscribers().stream().forEach(s -> {
            if (s.getEmailAddress().equals(subscriber)) {
                s.setActive(isSubscribed);
            }
        });
        if (isSubscribed) {
            subscription.setActive(isSubscribed);
        } else {
            if (subscription.getSubscribers().stream().noneMatch(Subscriber::isActive)) {
                subscription.setActive(isSubscribed);
            }
        }
        subscriptionsRepository.save(subscription);
    }
}
