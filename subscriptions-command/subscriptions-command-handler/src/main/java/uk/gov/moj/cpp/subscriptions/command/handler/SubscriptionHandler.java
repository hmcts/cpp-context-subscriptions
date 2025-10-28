package uk.gov.moj.cpp.subscriptions.command.handler;


import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.subscriptions.command.handler.helper.CommandHandlerHelper.appendMetaDataInEventStream;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Court.court;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.aggregate.SubscriptionAggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.Court;
import uk.gov.moj.cpp.subscriptions.json.schemas.Defendant;
import uk.gov.moj.cpp.subscriptions.json.schemas.Events;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.ActivateSubscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.Courts;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.CreateSubscriptionByAdmin;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.CreateSubscriptionByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeactivateSubscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.DeleteSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class SubscriptionHandler {


    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("subscriptions.command.handler.create-subscription-by-user")
    public void handleSubscriptionByUser(final Envelope<CreateSubscriptionByUser> envelope) throws EventStreamException {

        final CreateSubscriptionByUser subscription = envelope.payload();


        final EventStream eventStream = eventSource.getStreamById(subscription.getId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);

        final Stream<Object> events = subscriptionAggregate.createSubscriptionByUser(subscription.getOrganisationId(), as(subscription.getId(),subscription.getName(),
                subscription.getNowsOrEdts(), subscription.getFilter(), subscription.getEvents(), asList(subscription.getSubscriber()), subscription.getCourts()));

        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.create-subscription-by-admin")
    public void handleSubscription(final Envelope<CreateSubscriptionByAdmin> envelope) throws EventStreamException {

        final CreateSubscriptionByAdmin subscription = envelope.payload();


        final EventStream eventStream = eventSource.getStreamById(subscription.getId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);

        final Stream<Object> events = subscriptionAggregate.createSubscription(subscription.getOrganisationId(), as(subscription.getId(),subscription.getName(),
                subscription.getNowsOrEdts(), subscription.getFilter(), subscription.getEvents(),subscription.getSubscribers(), subscription.getCourts()));

        appendMetaDataInEventStream(envelope, eventStream, events);
    }


    @Handles("subscriptions.command.handler.activate-subscription")
    public void handleActivateSubscription(final Envelope<ActivateSubscription> envelope) throws EventStreamException {

        final ActivateSubscription subscription = envelope.payload();


        final EventStream eventStream = eventSource.getStreamById(subscription.getSubscriptionId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);

        final Stream<Object> events = subscriptionAggregate.activateSubscription(subscription.getSubscriptionId(), subscription.getOrganisationId());

        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.deactivate-subscription")
    public void handleDeactivateSubscription(final Envelope<DeactivateSubscription> envelope) throws EventStreamException {

        final DeactivateSubscription subscription = envelope.payload();


        final EventStream eventStream = eventSource.getStreamById(subscription.getSubscriptionId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);

        final Stream<Object> events = subscriptionAggregate.deactivateSubscription(subscription.getSubscriptionId(), subscription.getOrganisationId());

        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.delete-subscription")
    public void handleDeleteSubscription(final Envelope<DeleteSubscription> envelope) throws EventStreamException {

        final DeleteSubscription subscription = envelope.payload();


        final EventStream eventStream = eventSource.getStreamById(subscription.getSubscriptionId());
        final SubscriptionAggregate subscriptionAggregate = aggregateService.get(eventStream, SubscriptionAggregate.class);

        final Stream<Object> events = subscriptionAggregate.deleteSubscription(subscription.getSubscriptionId(), subscription.getOrganisationId());

        appendMetaDataInEventStream(envelope, eventStream, events);
    }


    private Subscription as(final UUID id, final String name, final List<String> nowsOrEdts, final uk.gov.moj.cpp.subscriptions.json.schemas.handler.Filter requestFilter,
                            final List<Events> requestEvents, final List<String> requestSubscribers, final List<Courts> requestCourts) {
        List<Subscribers> subscribers = new ArrayList<>();
        List<Court> courts = new ArrayList<>();
        Filter filter = null;
        if (isNotEmpty(requestSubscribers)) {
            subscribers = requestSubscribers.stream().map(s -> subscribers()
                    .withEmailAddress(s)
                    .withId(randomUUID())
                    .withActive(true)
                    .build())
                    .collect(Collectors.toList());
        }
        if (isNotEmpty(requestCourts)) {
            courts = requestCourts.stream().map(c -> court()
                    .withCourtId(c.getCourtId())
                    .withId(randomUUID())
                    .withName(c.getName())
                    .build())
                    .collect(Collectors.toList());
        }
        if(nonNull(requestFilter)) {
            Defendant defendant = null;
            if(nonNull(requestFilter.getDefendant())){
                defendant = defendant()
                        .withLastName(requestFilter.getDefendant().getLastName())
                        .withFirstName(requestFilter.getDefendant().getFirstName())
                        .withDateOfBirth(requestFilter.getDefendant().getDateOfBirth())
                        .build();
            }
            filter = filter()
                    .withFilterType(requestFilter.getFilterType())
                    .withId(randomUUID())
                    .withGender(requestFilter.getGender())
                    .withUrn(requestFilter.getUrn())
                    .withIsAdult(requestFilter.getIsAdult())
                    .withOffence(requestFilter.getOffence())
                    .withDefendant(defendant)
                    .build();
        }
        return subscription()
                .withId(id)
                .withName(name)
                .withActive(true)
                .withNowsOrEdts(nowsOrEdts)
                .withEvents(requestEvents)
                .withSubscribers(subscribers)
                .withCourts(courts)
                .withFilter(filter)
                .build();
    }



}
