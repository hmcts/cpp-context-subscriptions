package uk.gov.moj.cpp.subscriptions.aggregate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted.subscriberDeleted;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated.subscriptionActivated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated.subscriptionCreated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser.subscriptionCreatedByUser;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated.subscriptionDeactivated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted.subscriptionDeleted;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed.subscriptionSubscribed;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed.subscriptionUnsubscribed;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.Court;
import uk.gov.moj.cpp.subscriptions.json.schemas.Events;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleteFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriberDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionSubscribed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionUnsubscribed;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S1068", "squid:S1450", "squid:S2384"})
public class SubscriptionAggregate implements Aggregate {
    private static final long serialVersionUID = 100L;

    private Boolean active;

    private UUID id;

    private String name;

    private UUID organisationId;

    private List<Court> courts;

    private List<Events> events;

    private Filter filter;

    private List<String> nowsOrEdts;

    private List<Subscribers> subscribers;

    private boolean deleted;

    public Stream<Object> createSubscriptionByUser(final UUID organisationId, final Subscription subscription) {
        return apply(of(subscriptionCreatedByUser()
                .withSubscription(subscription)
                .withOrganisationId(organisationId)
                .build()
        ));
    }

    public Stream<Object> createSubscription(UUID organisationId, final Subscription subscription) {
        return apply(of(subscriptionCreated()
                .withSubscription(subscription)
                .withOrganisationId(organisationId)
                .build()
        ));
    }


    public Stream<Object> activateSubscription(final UUID subscriptionId, final UUID organisationId) {
        if (isDeleted()) {
            return Stream.empty();
        }

        return apply(of(subscriptionActivated()
                .withSubscriptionId(subscriptionId)
                .withOrganisationId(organisationId)
                .build()
        ));
    }

    public Stream<Object> deactivateSubscription(final UUID subscriptionId, final UUID organisationId) {
        if (isDeleted()) {
            return Stream.empty();
        }
        return apply(of(subscriptionDeactivated()
                .withSubscriptionId(subscriptionId)
                .withOrganisationId(organisationId)
                .build()
        ));
    }


    public Stream<Object> deleteSubscription(final UUID subscriptionId, final UUID organisationId) {
        return apply(of(subscriptionDeleted()
                .withSubscriptionId(subscriptionId)
                .withOrganisationId(organisationId)
                .build()
        ));
    }

    public Stream<Object> subscribe(final UUID subscriptionId, final UUID organisationId, final String subscriber) {
        if (isDeleted() || this.subscribers.stream().noneMatch(s -> s.getEmailAddress().equals(subscriber))) {
            return Stream.empty();
        }
        return apply(of(subscriptionSubscribed()
                .withSubscriptionId(subscriptionId)
                .withOrganisationId(organisationId)
                .withSubscriber(subscriber)
                .build()
        ));
    }

    public Stream<Object> unsubscribe(final UUID subscriptionId, final UUID organisationId, final String subscriber) {
        if (isDeleted() || this.subscribers.stream().noneMatch(s -> s.getEmailAddress().equals(subscriber))) {
            return Stream.empty();
        }
        return apply(of(subscriptionUnsubscribed()
                .withSubscriptionId(subscriptionId)
                .withOrganisationId(organisationId)
                .withSubscriber(subscriber)
                .build()
        ));
    }

    public Stream<Object> deleteSubscriber(final UUID subscriptionId, final UUID organisationId, final String subscriber) {
        if (isDeleted()) {
            return apply(of(buildDeleteFailedEvent(subscriptionId, organisationId, subscriber, "Subscription does not exist")
            ));
        }

        if (this.subscribers.stream().noneMatch(s -> s.getEmailAddress().equals(subscriber))) {
            return apply(of(buildDeleteFailedEvent(subscriptionId, organisationId, subscriber, "Subscriber does not subscribe to given subscription")));
        }

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(subscriberDeleted().withOrganisationId(organisationId).withSubscriber(subscriber).withSubscriptionId(subscriptionId).build());

        if (subscribers.size() == 1) {
            streamBuilder.add(subscriptionDeleted()
                    .withSubscriptionId(subscriptionId)
                    .withOrganisationId(organisationId)
                    .build());
        } else {
            final boolean hasOtherActiveSubscribers = subscribers.stream().anyMatch(s -> !s.getEmailAddress().equals(subscriber) && s.getActive());
            if (!hasOtherActiveSubscribers) {
                streamBuilder.add(subscriptionDeactivated()
                        .withSubscriptionId(subscriptionId)
                        .withOrganisationId(organisationId)
                        .build());
            }
        }
        return apply(streamBuilder.build());
    }

    private SubscriberDeleteFailed buildDeleteFailedEvent(final UUID subscriptionId, final UUID organisationId, final String subscriber, final String s) {
        return SubscriberDeleteFailed.subscriberDeleteFailed()
                .withSubscriptionId(subscriptionId)
                .withOrganisationId(organisationId)
                .withSubscriber(subscriber)
                .withReason(s)
                .build();
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(SubscriptionCreated.class).apply(subscriptionCreated -> {
                    this.id = subscriptionCreated.getSubscription().getId();
                    this.name = subscriptionCreated.getSubscription().getName();
                    this.active = subscriptionCreated.getSubscription().getActive();
                    this.organisationId = subscriptionCreated.getOrganisationId();
                    this.courts = subscriptionCreated.getSubscription().getCourts();
                    this.events = subscriptionCreated.getSubscription().getEvents();
                    this.filter = subscriptionCreated.getSubscription().getFilter();
                    this.nowsOrEdts = subscriptionCreated.getSubscription().getNowsOrEdts();
                    this.subscribers = subscriptionCreated.getSubscription().getSubscribers();
                }),
                when(SubscriptionCreatedByUser.class).apply(subscriptionCreatedByUser -> {
                    this.id = subscriptionCreatedByUser.getSubscription().getId();
                    this.name = subscriptionCreatedByUser.getSubscription().getName();
                    this.active = subscriptionCreatedByUser.getSubscription().getActive();
                    this.organisationId = subscriptionCreatedByUser.getOrganisationId();
                    this.courts = subscriptionCreatedByUser.getSubscription().getCourts();
                    this.events = subscriptionCreatedByUser.getSubscription().getEvents();
                    this.filter = subscriptionCreatedByUser.getSubscription().getFilter();
                    this.nowsOrEdts = subscriptionCreatedByUser.getSubscription().getNowsOrEdts();
                    this.subscribers = subscriptionCreatedByUser.getSubscription().getSubscribers();
                }),
                when(SubscriptionActivated.class).apply(this::processSubscriptionActivated),
                when(SubscriptionDeactivated.class).apply(this::processSubscriptionDeactivated),
                when(SubscriptionSubscribed.class).apply(e -> this.processSubscribeAndUnsubscribe(e.getSubscriber(), true)),
                when(SubscriptionUnsubscribed.class).apply(e -> this.processSubscribeAndUnsubscribe(e.getSubscriber(), false)),
                when(SendEmailRequested.class).apply(sendEmailRequested -> {
                }),
                when(SubscriptionDeleted.class).apply(e -> this.deleted = true),
                when(SubscriberDeleted.class).apply(e -> this.subscribers = this.subscribers.stream().filter(s -> !s.getEmailAddress().equals(e.getSubscriber())).collect(toList())),
                when(SubscriberDeleteFailed.class).apply(e -> {})

        );
    }



    private void processSubscribeAndUnsubscribe(final String emailAddress, final boolean isSubscribed) {
        final Optional<Subscribers> subscribersOptional = this.subscribers.stream()
                .filter(s -> s.getEmailAddress().equals(emailAddress))
                .map(subscriber -> subscribers()
                        .withValuesFrom(subscriber)
                        .withActive(isSubscribed)
                        .build())
                .findFirst();
        this.subscribers = this.subscribers
                .stream()
                .filter(s -> !s.getEmailAddress().equals(emailAddress))
                .collect(toList());
        if (subscribersOptional.isPresent()) {
            this.subscribers.add(subscribersOptional.get());
        }

        if (isSubscribed) {
            this.active = true;
        } else {
            if (this.subscribers.stream().noneMatch(Subscribers::getActive)) {
                this.active = false;
            }
        }
    }

    @SuppressWarnings({"squid:S1172"})
    private void processSubscriptionDeactivated(final SubscriptionDeactivated subscriptionDeactivated) {
        this.active = false;
        this.subscribers = this.subscribers.stream().map(subscriber -> subscribers()
                .withValuesFrom(subscriber)
                .withActive(false)
                .build())
                .collect(toList());
    }

    @SuppressWarnings({"squid:S1172"})
    private void processSubscriptionActivated(final SubscriptionActivated subscriptionActivated) {
        this.active = true;
        this.subscribers = this.subscribers.stream().map(subscriber -> subscribers()
                .withValuesFrom(subscriber)
                .withActive(true)
                .build())
                .collect(toList());
    }

    public Boolean getActive() {
        return active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public List<Subscribers> getSubscribers() {
        return subscribers;
    }

    public boolean isDeleted() {
        return deleted;
    }


}
