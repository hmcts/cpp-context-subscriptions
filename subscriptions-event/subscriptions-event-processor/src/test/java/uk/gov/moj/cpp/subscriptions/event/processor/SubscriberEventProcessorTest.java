package uk.gov.moj.cpp.subscriptions.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleteFailed.subscriberDeleteFailed;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted.subscriberDeleted;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated.subscriptionCreated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed.subscriptionSubscribed;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed.subscriptionUnsubscribed;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleteFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriberEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> captor;

    @InjectMocks
    private SubscriberEventProcessor subscriberEventProcessor;

    @Test
    public void shouldProcessPrivateSubscriptionSubscribeAndPublicSubscribedRaised() {


        final SubscriptionSubscribed subscriptionCreated = subscriptionSubscribed().build();

        final Envelope<SubscriptionSubscribed> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-subscribed"),
                subscriptionCreated);

        subscriberEventProcessor.handleSubscribe(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-subscribed-successfully"));
    }

    @Test
    public void shouldProcessPrivateSubscriptionUnsubscribeAndPublicUnsubscribedRaised() {


        final SubscriptionUnsubscribed subscriptionCreated = subscriptionUnsubscribed().build();

        final Envelope<SubscriptionUnsubscribed> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-unsubscribed"),
                subscriptionCreated);

        subscriberEventProcessor.handleUnsubscribe(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-unsubscribed-successfully"));
    }

    @Test
    public void shouldProcessPrivateDeleteSubscribeAndPublicDeleteSubscribedRaised() {


        final SubscriberDeleted subscriptionCreated = subscriberDeleted().build();

        final Envelope<SubscriberDeleted> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscriber-deleted"),
                subscriptionCreated);

        subscriberEventProcessor.handleDeletedSubscribe(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscriber-deleted"));
    }

    @Test
    public void shouldProcessPrivateDeleteSubscribeFailedAndPublicDeleteSubscribeFailedRaised() {


        final SubscriberDeleteFailed subscriptionCreated = subscriberDeleteFailed().build();

        final Envelope<SubscriberDeleteFailed> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscriber-delete-failed"),
                subscriptionCreated);

        subscriberEventProcessor.handleDeletedSubscribeFailed(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscriber-delete-failed"));
    }

}