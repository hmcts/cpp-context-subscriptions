package uk.gov.moj.cpp.subscriptions.query.converter;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.moj.cpp.subscriptions.json.schemas.Events;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;
import uk.gov.moj.cpp.subscriptions.persistence.entity.NowsEdt;

public class SubscriptionConverter implements Converter<uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription, Subscription> {

    private final String filterBySubscription;
    private CourtConverter courtConverter = new CourtConverter();
    private FilterConverter filterConverter = new FilterConverter();
    private SubscriberConverter subscriberConverter = new SubscriberConverter();

    public SubscriptionConverter(final String filterBySubscriber) {
        this.filterBySubscription = filterBySubscriber;
    }

    @Override
    public Subscription convert(uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription source) {
        final Subscription.Builder builder = subscription()
                .withId(source.getId())
                .withActive(source.isActive())
                .withName(source.getName())
                .withCourts(source.getCourts().stream().map(a -> courtConverter.convert(a)).collect(toList()))
                .withEvents(source.getEvents().stream().map(a -> Events.valueOf(a.getName().name())).collect(toList()))
                .withFilter(filterConverter.convert(source.getFilter()))
                .withNowsOrEdts(source.getNowsEdts().stream().map(NowsEdt::getName).collect(toList()));
        if (nonNull(filterBySubscription)) {
            builder.withSubscribers(source.getSubscribers().stream().filter(s -> s.getEmailAddress().equals(filterBySubscription)).map(a -> subscriberConverter.convert(a)).collect(toList()));
        } else {
            builder.withSubscribers(source.getSubscribers().stream().map(a -> subscriberConverter.convert(a)).collect(toList()));
        }
        return builder.build();
    }
}
