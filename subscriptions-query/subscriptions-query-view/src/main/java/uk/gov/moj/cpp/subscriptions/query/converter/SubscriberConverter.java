package uk.gov.moj.cpp.subscriptions.query.converter;

import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;

import uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers;

public class SubscriberConverter implements Converter<uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber, Subscribers> {


    @Override
    public Subscribers convert(uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber subscriber) {
        return subscribers()
                .withId(subscriber.getId())
                .withActive(subscriber.isActive())
                .withEmailAddress(subscriber.getEmailAddress())
                .build();
    }
}
