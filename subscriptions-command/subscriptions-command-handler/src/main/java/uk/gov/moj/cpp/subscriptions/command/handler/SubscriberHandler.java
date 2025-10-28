package uk.gov.moj.cpp.subscriptions.command.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.subscriptions.command.handler.helper.CommandHandlerHelper.appendMetaDataInEventStream;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.aggregate.SubscriptionAggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeleteSubscriber;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.Subscribe;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.Unsubscribe;

import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class SubscriberHandler {


    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("subscriptions.command.handler.subscribe")
    public void handleSubscribe(final Envelope<Subscribe> envelope) throws EventStreamException {
        final Subscribe subscribe = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(subscribe.getSubscriptionId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);
        final Stream<Object> events = subscriptionAggregate.subscribe(subscribe.getSubscriptionId(), subscribe.getOrganisationId(), subscribe.getSubscriber());
        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.unsubscribe")
    public void handleUnsubscribe(final Envelope<Unsubscribe> envelope) throws EventStreamException {
        final Unsubscribe subscribe = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(subscribe.getSubscriptionId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);
        final Stream<Object> events = subscriptionAggregate.unsubscribe(subscribe.getSubscriptionId(), subscribe.getOrganisationId(), subscribe.getSubscriber());
        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.delete-subscriber")
    public void handleDeleteSubscriber(final Envelope<DeleteSubscriber> envelope) throws EventStreamException {
        final DeleteSubscriber subscribe = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(subscribe.getSubscriptionId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);
        final Stream<Object> events = subscriptionAggregate.deleteSubscriber(subscribe.getSubscriptionId(), subscribe.getOrganisationId(), subscribe.getSubscriber());
        appendMetaDataInEventStream(envelope, eventStream, events);
    }
}
