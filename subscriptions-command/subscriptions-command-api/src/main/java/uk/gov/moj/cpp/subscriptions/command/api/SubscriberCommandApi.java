package uk.gov.moj.cpp.subscriptions.command.api;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.FeatureControl;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.subscriptions.command.api.service.UserAndGroupsService;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class SubscriberCommandApi {

    private static final String ORGANISATION_ID = "organisationId";
    private static final String SUBSCRIBER = "subscriber";
    private static final String EMAIL = "email";
    private static final String FEATURE_KEY = "subscriptionsPortal";


    @Inject
    private Sender sender;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    @Handles("subscriptions.command.subscribe")
    @FeatureControl(FEATURE_KEY)
    public void subscribe(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.subscribe")
                .build();

        final JsonObject userDetails = userAndGroupsService.getUserDetails(envelope);
        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userDetails))
                .add(SUBSCRIBER, userDetails.getString(EMAIL))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }

    @Handles("subscriptions.command.unsubscribe")
    @FeatureControl(FEATURE_KEY)
    public void unsubscribe(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.unsubscribe")
                .build();

        final JsonObject userDetails = userAndGroupsService.getUserDetails(envelope);
        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userDetails))
                .add(SUBSCRIBER, userDetails.getString(EMAIL))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }

    @Handles("subscriptions.command.delete-subscriber")
    @FeatureControl(FEATURE_KEY)
    public void deleteSubscribe(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.delete-subscriber")
                .build();

        final JsonObject userDetails = userAndGroupsService.getUserDetails(envelope);
        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userDetails))
                .add(SUBSCRIBER, userDetails.getString(EMAIL))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }

    private String extractOrganisationId(final JsonObject response) {
        if(response.containsKey(ORGANISATION_ID) && nonNull(response.getString(ORGANISATION_ID))) {
            return response.getString(ORGANISATION_ID);
        }
        throw new BadRequestException("User does not belong to any organisation");
    }

}
