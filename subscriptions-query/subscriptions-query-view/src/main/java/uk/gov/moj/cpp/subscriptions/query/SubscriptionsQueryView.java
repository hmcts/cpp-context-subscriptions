package uk.gov.moj.cpp.subscriptions.query;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.persistence.repository.SubscriptionsRepository;
import uk.gov.moj.cpp.subscriptions.query.converter.SubscriptionConverter;

import java.util.UUID;

import javax.inject.Inject;


public class SubscriptionsQueryView {

    public static final String SUBSCRIPTIONS = "subscriptions";
    @Inject
    private SubscriptionsRepository subscriptionsRepository;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;


    public JsonEnvelope retrieveSubscriptions(final JsonEnvelope envelope, final UUID organisationId) {
        final SubscriptionConverter subscriptionConverter = new SubscriptionConverter(null);

        return envelopeFrom(envelope.metadata(),
                createObjectBuilder()
                        .add(SUBSCRIPTIONS, objectToJsonValueConverter.convert(
                                subscriptionsRepository.findByOrganisationId(organisationId)
                                        .stream()
                                        .map(subscriptionConverter::convert)
                                        .collect(toList())
                        ))
                        .build());
    }

    public JsonEnvelope retrieveSubscriptions(final JsonEnvelope envelope, final UUID organisationId, final String email) {
        final SubscriptionConverter subscriptionConverter = new SubscriptionConverter(email);
        return envelopeFrom(envelope.metadata(),
                createObjectBuilder()
                        .add(SUBSCRIPTIONS, objectToJsonValueConverter.convert(
                                subscriptionsRepository.findByOrganisationIdAndSubscriber(organisationId, email)
                                        .stream()
                                        .map(subscriptionConverter::convert)
                                        .collect(toList())
                        ))
                        .build());
    }

    public JsonEnvelope retrieveSubscriptionsByCourtId(final JsonEnvelope envelope, final UUID courtId) {
        final SubscriptionConverter subscriptionConverter = new SubscriptionConverter(null);

        return envelopeFrom(envelope.metadata(),
                createObjectBuilder()
                        .add(SUBSCRIPTIONS, objectToJsonValueConverter.convert(
                                subscriptionsRepository.findByCourtId(courtId)
                                        .stream()
                                        .map(subscriptionConverter::convert)
                                        .collect(toList())
                        ))
                        .build());
    }
}
