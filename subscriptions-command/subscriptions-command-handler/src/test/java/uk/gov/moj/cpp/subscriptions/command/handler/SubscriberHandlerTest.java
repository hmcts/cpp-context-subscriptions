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
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeleteSubscriber.deleteSubscriber;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.Subscribe.subscribe;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.Unsubscribe.unsubscribe;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.aggregate.SubscriptionAggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeleteSubscriber;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.Subscribe;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.Unsubscribe;

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
public class SubscriberHandlerTest {


    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_SUBSCRIBE_SUBSCRIPTION = "subscriptions.command.handler.subscribe";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_UNSUBSCRIBE_SUBSCRIPTION = "subscriptions.command.handler.unsubscribe";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_DELETE_SUBSCRIBER = "subscriptions.command.handler.delete-subscriber";
    private static final UUID SUBSCRIPTION_ID = randomUUID();

    @InjectMocks
    private SubscriberHandler subscriberHandler;

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
            SubscriptionSubscribed.class,
            SubscriptionUnsubscribed.class,
            SubscriptionDeleted.class,
            SubscriberDeleted.class
    );

    @Test
    public void shouldHandleSubscribeSubscriptionCommand() {
        assertThat(subscriberHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleSubscribe")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_SUBSCRIBE_SUBSCRIPTION)));
    }

    @Test
    public void shouldHandleUnsubscribeSubscriptionCommand() {
        assertThat(subscriberHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleUnsubscribe")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_UNSUBSCRIBE_SUBSCRIPTION)));
    }

    @Test
    public void shouldHandleDeleteSubscriberCommand() {
        assertThat(subscriberHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleDeleteSubscriber")
                        .thatHandles(SUBSCRIPTIONS_COMMAND_HANDLER_DELETE_SUBSCRIBER)));
    }

    @Test
    public void shouldProcessDeleteSubscribeRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(SUBSCRIPTION_ID)
                        .withActive(true)
                        .withSubscribers(asList(subscribers().withId(randomUUID()).withEmailAddress("test").withActive(true).build(),
                                subscribers().withId(randomUUID()).withEmailAddress("test1").withActive(true).build())).build()).build();
        subscriptionAggregate.apply(subscriptionCreated);

        final Envelope<DeleteSubscriber> subscribeSubscriptionHandlerEnvelope = createDeleteSubscribeHandlerEnvelope();
        subscriberHandler.handleDeleteSubscriber(subscribeSubscriptionHandlerEnvelope);

        verifySubscriberHandlerResults("subscriptions.event.subscriber-deleted", "$.subscriptionId");
    }

    @Test
    public void shouldProcessSubscribeRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(SUBSCRIPTION_ID)
                        .withActive(true)
                        .withSubscribers(asList(subscribers().withId(randomUUID()).withEmailAddress("test").withActive(true).build())).build()).build();
        subscriptionAggregate.apply(subscriptionCreated);

        final Envelope<Subscribe> subscribeSubscriptionHandlerEnvelope = createSubscribeSubscriptionHandlerEnvelope();
        subscriberHandler.handleSubscribe(subscribeSubscriptionHandlerEnvelope);

        verifySubscriberHandlerResults("subscriptions.event.subscription-subscribed", "$.subscriptionId");
    }

    @Test
    public void shouldProcessUnsubscribeRaisePrivateEvent() throws Exception {
        subscriptionAggregate = spy(SubscriptionAggregate.class);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SubscriptionAggregate.class)).thenReturn(subscriptionAggregate);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(SUBSCRIPTION_ID)
                        .withActive(true)
                        .withSubscribers(asList(subscribers().withId(randomUUID()).withEmailAddress("test").withActive(true).build())).build()).build();
        subscriptionAggregate.apply(subscriptionCreated);

        final Envelope<Unsubscribe> unsubscribeEnvelope = createUnsubscribeSubscriptionHandlerEnvelope();
        subscriberHandler.handleUnsubscribe(unsubscribeEnvelope);

        verifySubscriberHandlerResults("subscriptions.event.subscription-unsubscribed", "$.subscriptionId");
    }

    private void verifySubscriberHandlerResults(String eventName, final String jsonPath) throws EventStreamException {
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

    private Envelope<Subscribe> createSubscribeSubscriptionHandlerEnvelope() {
        final Subscribe subscribe = subscribe()
                .withSubscriptionId(SUBSCRIPTION_ID)
                .withSubscriber("test")
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(subscribe)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_SUBSCRIBE_SUBSCRIPTION)
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope<Unsubscribe> createUnsubscribeSubscriptionHandlerEnvelope() {
        final Unsubscribe subscribe = unsubscribe()
                .withSubscriptionId(SUBSCRIPTION_ID)
                .withSubscriber("test")
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(subscribe)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_UNSUBSCRIBE_SUBSCRIPTION)
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope<DeleteSubscriber> createDeleteSubscribeHandlerEnvelope() {
        final DeleteSubscriber subscribe = deleteSubscriber()
                .withSubscriptionId(SUBSCRIPTION_ID)
                .withSubscriber("test")
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(subscribe)
                .withName(SUBSCRIPTIONS_COMMAND_HANDLER_SUBSCRIBE_SUBSCRIPTION)
                .withMetadataFrom(requestEnvelope);
    }


}