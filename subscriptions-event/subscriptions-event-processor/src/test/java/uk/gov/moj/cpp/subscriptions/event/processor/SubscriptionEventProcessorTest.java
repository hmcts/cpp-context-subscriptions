package uk.gov.moj.cpp.subscriptions.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated.subscriptionActivated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated.subscriptionCreated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser.subscriptionCreatedByUser;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated.subscriptionDeactivated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted.subscriptionDeleted;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> captor;

    @InjectMocks
    private SubscriptionEventProcessor subscriptionEventProcessor;

    @Test
    public void shouldProcessPrivateCreateSubscriptionAndPublicCreateSubscriptionRaised() {


        final SubscriptionCreated subscriptionCreated = subscriptionCreated().build();

        final Envelope<SubscriptionCreated> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-created"),
                subscriptionCreated);

        subscriptionEventProcessor.handleCreateSubscription(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-created"));
    }

    @Test
    public void shouldProcessPrivateActivateSubscriptionAndPublicActivateSubscriptionRaised() {


        final SubscriptionActivated subscriptionActivated = subscriptionActivated().build();

        final Envelope<SubscriptionActivated> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-activated"),
                subscriptionActivated);

        subscriptionEventProcessor.handleSubscriptionActivated(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-activated-successfully"));
    }

    @Test
    public void shouldProcessPrivateDeactivateSubscriptionAndPublicDeactivateSubscriptionRaised() {


        final SubscriptionDeactivated subscriptionDeactivated = subscriptionDeactivated().build();

        final Envelope<SubscriptionDeactivated> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-deactivated"),
                subscriptionDeactivated);

        subscriptionEventProcessor.handleSubscriptionDeactivated(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-deactivated-successfully"));
    }

    @Test
    public void shouldProcessPrivateDeleteSubscriptionAndPublicDeleteSubscriptionRaised() {


        final SubscriptionDeleted subscriptionDeactivated = subscriptionDeleted().build();

        final Envelope<SubscriptionDeleted> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-deleted"),
                subscriptionDeactivated);

        subscriptionEventProcessor.handleSubscriptionDeleted(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-deleted-successfully"));
    }

    @Test
    public void shouldProcessPrivateCreateSubscriptionByUserAndPublicCreateSubscriptionRaised() {


        final SubscriptionCreatedByUser subscriptionCreated = subscriptionCreatedByUser().build();

        final Envelope<SubscriptionCreatedByUser> envelope = envelopeFrom(
                metadataWithRandomUUID("subscriptions.event.subscription-created-by-user"),
                subscriptionCreated);

        subscriptionEventProcessor.handleCreateSubscriptionByUser(envelope);

        verify(sender).send(captor.capture());

        final Envelope<?> messageEnvelope = captor.getValue();
        assertThat(messageEnvelope.metadata().name(), is("public.subscriptions.event.subscription-created-by-user-successfully"));
    }
}
