package uk.gov.moj.cpp.subscriptions.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.Events;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;
import uk.gov.moj.cpp.subscriptions.persistence.constants.EventType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.Gender;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Court;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Event;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Filter;
import uk.gov.moj.cpp.subscriptions.persistence.entity.NowsEdt;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;
import uk.gov.moj.cpp.subscriptions.persistence.repository.SubscriptionsRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SubscriptionEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private SubscriptionsRepository subscriptionsRepository;


    @Handles("subscriptions.event.subscription-created")
    public void handleSubscriptionCreated(final JsonEnvelope event) {

        final SubscriptionCreated subscriptionCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionCreated.class);
        final uk.gov.moj.cpp.subscriptions.json.schemas.Subscription subscription = subscriptionCreated.getSubscription();
        subscriptionsRepository.save(buildSubscription(subscription.getId(), subscription.getName(), subscriptionCreated.getOrganisationId(),
                subscription.getCourts(), subscription.getEvents(), subscription.getFilter(), subscription.getNowsOrEdts(), subscription.getSubscribers()));

    }

    @Handles("subscriptions.event.subscription-activated")
    public void handleSubscriptionActivated(final JsonEnvelope event) {
        final SubscriptionActivated subscriptionActivated = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionActivated.class);

        activateDeactivateSubscription(subscriptionActivated.getSubscriptionId(), true);
    }

    @Handles("subscriptions.event.subscription-deactivated")
    public void handleSubscriptionDeactivated(final JsonEnvelope event) {
        final SubscriptionDeactivated subscriptionDeactivated = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionDeactivated.class);

        activateDeactivateSubscription(subscriptionDeactivated.getSubscriptionId(), false);
    }

    @Handles("subscriptions.event.subscription-deleted")
    public void handleSubscriptionDeleted(final JsonEnvelope event) {
        final SubscriptionDeleted subscriptionDeleted = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionDeleted.class);
        final Subscription subscription = subscriptionsRepository.findBy(subscriptionDeleted.getSubscriptionId());
        subscriptionsRepository.remove(subscription);
    }

    @Handles("subscriptions.event.subscription-created-by-user")
    public void handleSubscriptionCreatedByUser(final JsonEnvelope event) {

        final SubscriptionCreatedByUser subscriptionCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), SubscriptionCreatedByUser.class);
        final uk.gov.moj.cpp.subscriptions.json.schemas.Subscription subscription = subscriptionCreated.getSubscription();
        subscriptionsRepository.save(buildSubscription(subscription.getId(), subscription.getName(), subscriptionCreated.getOrganisationId(),
                subscription.getCourts(), subscription.getEvents(), subscription.getFilter(), subscription.getNowsOrEdts(), subscription.getSubscribers()));

    }


    private void activateDeactivateSubscription(final UUID subscriptionId, final boolean activate) {
        final Subscription subscription = subscriptionsRepository.findBy(subscriptionId);
        subscription.setActive(activate);
        if (isNotEmpty(subscription.getSubscribers())) {
            subscription.getSubscribers().stream().forEach(subscriber -> subscriber.setActive(activate));
        }
        subscriptionsRepository.save(subscription);
    }



    private Subscription buildSubscription(final UUID id, final String name, final UUID organisationId, final List<uk.gov.moj.cpp.subscriptions.json.schemas.Court> courts,
                                           final List<Events> events, final uk.gov.moj.cpp.subscriptions.json.schemas.Filter filter, final List<String> nowsOrEdts, final List<Subscribers> subscribers) {

        final Subscription subscription = builder()
                .withId(id)
                .withName(name)
                .withOrganisationId(organisationId)
                .withActive(true)
                .build();


        subscription.setCourts(buildCourts(courts, subscription));
        subscription.setNowsEdts(buildNowsEdts(nowsOrEdts, subscription));
        subscription.setEvents(buildEvents(events, subscription));
        subscription.setFilter(buildFilter(filter));
        subscription.setSubscribers(buildSubscribers(subscribers, subscription));
        return subscription;
    }

    @SuppressWarnings({"squid:S1168"})
    private Set<Subscriber> buildSubscribers(final List<Subscribers> subscribers, final Subscription subscription) {
        if (isEmpty(subscribers)) {
            return null;
        }
        return subscribers.stream().map(subscriber -> Subscriber.builder()
                .withId(subscriber.getId())
                .withEmailAddress(subscriber.getEmailAddress())
                .withActive(true)
                .withSubscription(subscription)
                .build())
                .collect(toSet());
    }

    @SuppressWarnings({"squid:S1168"})
    private Filter buildFilter(final uk.gov.moj.cpp.subscriptions.json.schemas.Filter filter) {
        if (isNull(filter)) {
            return null;
        }
        final Filter.FiltersBuilder filtersBuilder = Filter.builder()
                .withId(filter.getId())
                .withFilterType(FilterType.valueOf(filter.getFilterType().name()))
                .withAdult(filter.getIsAdult())
                .withUrn(filter.getUrn())
                .withOffence(filter.getOffence());
        if (nonNull(filter.getGender())) {
            filtersBuilder.withGender(Gender.valueOf(filter.getGender().name()));
        }

        if (nonNull(filter.getDefendant())) {
            filtersBuilder
                    .withDefendantFirstName(filter.getDefendant().getFirstName())
                    .withDefendantLastName(filter.getDefendant().getLastName())
                    .withDateOfBirth(filter.getDefendant().getDateOfBirth());
        }
        return filtersBuilder.build();
    }

    @SuppressWarnings({"squid:S1168"})
    private Set<Event> buildEvents(final List<Events> events, final Subscription subscription) {
        if (isEmpty(events)) {
            return null;
        }
        return events.stream().map(event -> Event.builder()
                .withId(randomUUID())
                .withName(EventType.valueOf(event.name()))
                .withSubscription(subscription)
                .build())
                .collect(toSet());
    }

    @SuppressWarnings({"squid:S1168"})
    private Set<NowsEdt> buildNowsEdts(final List<String> nowsOrEdts, final Subscription subscription) {
        if (isEmpty(nowsOrEdts)) {
            return null;
        }
        return nowsOrEdts.stream().map(nowsedts -> NowsEdt.builder()
                .withId(randomUUID())
                .withName(nowsedts)
                .withSubscription(subscription)
                .build())
                .collect(toSet());
    }

    @SuppressWarnings({"squid:S1168"})
    private Set<Court> buildCourts(final List<uk.gov.moj.cpp.subscriptions.json.schemas.Court> courts, final Subscription subscription) {
        if (isEmpty(courts)) {
            return null;
        }
        return courts.stream().map(court -> Court.builder()
                .withId(court.getId())
                .withCourtId(court.getCourtId())
                .withName(court.getName())
                .withSubscription(subscription)
                .build())
                .collect(toSet());
    }
}
