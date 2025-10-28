package uk.gov.moj.cpp.subscriptions.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.subscriptions.event.processor.ProcessorHelper.sendPublicEvent;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class SubscriptionEventProcessor {
    private static final String SUBSCRIPTION_CREATED_PUBLIC_EVENT = "public.subscriptions.event.subscription-created";
    private static final String SUBSCRIPTION_CREATED_BY_USER_PUBLIC_EVENT = "public.subscriptions.event.subscription-created-by-user-successfully";
    private static final String SUBSCRIPTION_ACTIVATED_PUBLIC_EVENT = "public.subscriptions.event.subscription-activated-successfully";
    private static final String SUBSCRIPTION_DEACTIVATED_PUBLIC_EVENT = "public.subscriptions.event.subscription-deactivated-successfully";
    private static final String SUBSCRIPTION_DELETED_PUBLIC_EVENT = "public.subscriptions.event.subscription-deleted-successfully";


    @Inject
    private Sender sender;

    @Handles("subscriptions.event.subscription-created-by-user")
    public void handleCreateSubscriptionByUser(final Envelope<SubscriptionCreatedByUser> envelope) {
        sendPublicEvent(SUBSCRIPTION_CREATED_BY_USER_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }

    @Handles("subscriptions.event.subscription-created")
    public void handleCreateSubscription(final Envelope<SubscriptionCreated> envelope) {
        sendPublicEvent(SUBSCRIPTION_CREATED_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }

    @Handles("subscriptions.event.subscription-activated")
    public void handleSubscriptionActivated(final Envelope<SubscriptionActivated> envelope) {
        sendPublicEvent(SUBSCRIPTION_ACTIVATED_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);

    }

    @Handles("subscriptions.event.subscription-deactivated")
    public void handleSubscriptionDeactivated(final Envelope<SubscriptionDeactivated> envelope) {
        sendPublicEvent(SUBSCRIPTION_DEACTIVATED_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);
    }

    @Handles("subscriptions.event.subscription-deleted")
    public void handleSubscriptionDeleted(final Envelope<SubscriptionDeleted> envelope) {
        sendPublicEvent(SUBSCRIPTION_DELETED_PUBLIC_EVENT, envelope.metadata(), envelope.payload(), sender);

    }

}
