package uk.gov.moj.cpp.subscriptions.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.subscriptions.event.processor.ProcessorHelper.sendPublicEvent;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleteFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class SubscriberEventProcessor {
    private static final String SUBSCRIBER_SUBSCRIBE_PUBLIC_EVENT = "public.subscriptions.event.subscription-subscribed-successfully";
    private static final String SUBSCRIBER_UNSUBSCRIBE_PUBLIC_EVENT = "public.subscriptions.event.subscription-unsubscribed-successfully";
    private static final String SUBSCRIBER_DELETE_SUBSCRIBE_PUBLIC_EVENT = "public.subscriptions.event.subscriber-deleted";
    private static final String SUBSCRIBER_DELETE_SUBSCRIBE_FAILED_PUBLIC_EVENT = "public.subscriptions.event.subscriber-delete-failed";


    @Inject
    private Sender sender;

    @Handles("subscriptions.event.subscription-subscribed")
    public void handleSubscribe(final Envelope<SubscriptionSubscribed> envelope) {
        sendPublicEvent(SUBSCRIBER_SUBSCRIBE_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }

    @Handles("subscriptions.event.subscription-unsubscribed")
    public void handleUnsubscribe(final Envelope<SubscriptionUnsubscribed> envelope) {
        sendPublicEvent(SUBSCRIBER_UNSUBSCRIBE_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }

    @Handles("subscriptions.event.subscriber-deleted")
    public void handleDeletedSubscribe(final Envelope<SubscriberDeleted> envelope) {
        sendPublicEvent(SUBSCRIBER_DELETE_SUBSCRIBE_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }

    @Handles("subscriptions.event.subscriber-delete-failed")
    public void handleDeletedSubscribeFailed(final Envelope<SubscriberDeleteFailed> envelope) {
        sendPublicEvent(SUBSCRIBER_DELETE_SUBSCRIBE_FAILED_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }
}
