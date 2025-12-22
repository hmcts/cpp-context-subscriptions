
package uk.gov.moj.cpp.subscriptions.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated.subscriptionCreated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.ActivateSubscription.activateSubscription;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.CreateSubscriptionByAdmin.createSubscriptionByAdmin;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.CreateSubscriptionByUser.createSubscriptionByUser;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeactivateSubscription.deactivateSubscription;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeleteSubscription.deleteSubscription;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.aggregate.SubscriptionAggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.ActivateSubscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.CreateSubscriptionByAdmin;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.CreateSubscriptionByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeactivateSubscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeleteSubscription;

import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionHandlerTest {


    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_CREATE_SUBSCRIPTION_BY_ADMIN = "subscriptions.command.handler.create-subscription-by-admin";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_CREATE_SUBSCRIPTION_BY_USER = "subscriptions.command.handler.create-subscription-by-user";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_ACTIVATE_SUBSCRIPTION_BY_ADMIN = "subscriptions.command.handler.activate-subscription";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_DEACTIVATE_SUBSCRIPTION_BY_ADMIN = "subscriptions.command.handler.deactivate-subscription";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_DELETE_SUBSCRIPTION_BY_ADMIN = "subscriptions.command.handler.delete-subscription";
    private static final UUID SUBSCRIPTION_ID = randomUUID();

    @InjectMocks
    private SubscriptionHandler subscriptionHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private SubscriptionAggregate subscriptionAggregate;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            SubscriptionCreated.class,
            SubscriptionActivated.class,
            SubscriptionDeactivated.class,
            SubscriptionDeleted.class,
            SubscriptionCreatedByUser.class
    );


    @Test
    public void shouldHandleAddSubscriptionCommand() {
        assertThat(subscriptionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleSubscription")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_CREATE_SUBSCRIPTION_BY_ADMIN)));
    }

    @Test
    public void shouldHandleSubscriptionByUserCommand() {
        assertThat(subscriptionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleSubscriptionByUser")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_CREATE_SUBSCRIPTION_BY_USER)));
    }

    @Test
    public void shouldHandleActivateSubscriptionCommand() {
        assertThat(subscriptionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleActivateSubscription")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_ACTIVATE_SUBSCRIPTION_BY_ADMIN)));
    }

    @Test
    public void shouldHandleDeactivateSubscriptionCommand() {
        assertThat(subscriptionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleDeactivateSubscription")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_DEACTIVATE_SUBSCRIPTION_BY_ADMIN)));
    }

    @Test
    public void shouldHandleDeleteSubscriptionCommand() {
        assertThat(subscriptionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleDeleteSubscription")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_DELETE_SUBSCRIPTION_BY_ADMIN)));
    }

    @Test
    public void shouldProcessCreateSubscriptionByUserRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final Envelope<CreateSubscriptionByUser> envelope = createCreateSubscriptionByUserHandlerEnvelope();

        subscriptionHandler.handleSubscriptionByUser(envelope);

        verifySubscriptionHandlerResults("subscriptions.event.subscription-created-by-user", "$.subscription.id");
    }

    @Test
    public void shouldProcessCreateSubscriptionRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final Envelope<CreateSubscriptionByAdmin> envelope = createCreateSubscriptionHandlerEnvelope();

        subscriptionHandler.handleSubscription(envelope);

        verifySubscriptionHandlerResults("subscriptions.event.subscription-created", "$.subscription.id");
    }


    @Test
    public void shouldProcessActivateSubscriptionRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(SUBSCRIPTION_ID)
                        .withSubscribers(asList(subscribers().withId(randomUUID()).build())).build()).build();
        subscriptionAggregate.apply(subscriptionCreated);

        final Envelope<ActivateSubscription> activateSubscriptionHandlerEnvelope = createActivateSubscriptionHandlerEnvelope();
        subscriptionHandler.handleActivateSubscription(activateSubscriptionHandlerEnvelope);

        verifySubscriptionHandlerResults("subscriptions.event.subscription-activated", "$.subscriptionId");
    }

    @Test
    public void shouldProcessDeactivateSubscriptionRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(SUBSCRIPTION_ID)
                        .withSubscribers(asList(subscribers().withId(randomUUID()).build())).build()).build();
        subscriptionAggregate.apply(subscriptionCreated);

        final Envelope<DeactivateSubscription> deactivateSubscriptionEnvelope = creatDeactivateSubscriptionHandlerEnvelope();
        subscriptionHandler.handleDeactivateSubscription(deactivateSubscriptionEnvelope);

        verifySubscriptionHandlerResults("subscriptions.event.subscription-deactivated", "$.subscriptionId");
    }

    @Test
    public void shouldProcessDeleteSubscriptionRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(SUBSCRIPTION_ID)
                        .withSubscribers(asList(subscribers().withId(randomUUID()).build())).build()).build();
        subscriptionAggregate.apply(subscriptionCreated);

        final Envelope<DeleteSubscription> deleteSubscriptionEnvelope = creatDeleteSubscriptionHandlerEnvelope();
        subscriptionHandler.handleDeleteSubscription(deleteSubscriptionEnvelope);

        verifySubscriptionHandlerResults("subscriptions.event.subscription-deleted", "$.subscriptionId");
    }


    private void verifySubscriptionHandlerResults(String eventName, final String jsonPath) throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(eventName),
                        payload().isJson(allOf(
                                withJsonPath(jsonPath, Matchers.is(SUBSCRIPTION_ID.toString()))
                                )
                        ))

                )
        );
    }

    private Envelope<CreateSubscriptionByUser> createCreateSubscriptionByUserHandlerEnvelope() {
        final CreateSubscriptionByUser createSubscriptionByUser = createSubscriptionByUser()
                .withId(SUBSCRIPTION_ID)
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(createSubscriptionByUser)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_CREATE_SUBSCRIPTION_BY_USER)
                .withMetadataFrom(requestEnvelope);
    }


    private Envelope<CreateSubscriptionByAdmin> createCreateSubscriptionHandlerEnvelope() {
        final CreateSubscriptionByAdmin createSubscriptionByAdmin = createSubscriptionByAdmin()
                .withId(SUBSCRIPTION_ID)
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(createSubscriptionByAdmin)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_CREATE_SUBSCRIPTION_BY_ADMIN)
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope<ActivateSubscription> createActivateSubscriptionHandlerEnvelope() {
        final ActivateSubscription activateSubscription = activateSubscription()
                .withSubscriptionId(SUBSCRIPTION_ID)
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(activateSubscription)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_ACTIVATE_SUBSCRIPTION_BY_ADMIN)
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope<DeactivateSubscription> creatDeactivateSubscriptionHandlerEnvelope() {
        final DeactivateSubscription deactivateSubscription = deactivateSubscription()
                .withSubscriptionId(SUBSCRIPTION_ID)
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(deactivateSubscription)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_ACTIVATE_SUBSCRIPTION_BY_ADMIN)
                .withMetadataFrom(requestEnvelope);
    }


    private Envelope<DeleteSubscription> creatDeleteSubscriptionHandlerEnvelope() {
        final DeleteSubscription deactivateSubscription = deleteSubscription()
                .withSubscriptionId(SUBSCRIPTION_ID)
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(deactivateSubscription)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_DELETE_SUBSCRIPTION_BY_ADMIN)
                .withMetadataFrom(requestEnvelope);
    }

}
