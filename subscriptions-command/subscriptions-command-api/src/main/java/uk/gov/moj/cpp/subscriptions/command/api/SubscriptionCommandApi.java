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
public class SubscriptionCommandApi {

    private static final String ORGANISATION_ID = "organisationId";
    private static final String SUBSCRIBER = "subscriber";
    private static final String EMAIL = "email";
    private static final String FEATURE_KEY = "subscriptionsPortal";

    @Inject
    private Sender sender;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    @Handles("subscriptions.command.create-subscription-by-admin")
    @FeatureControl(FEATURE_KEY)
    public void subscription(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.create-subscription-by-admin")
                .build();

        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userAndGroupsService.getUserDetails(envelope)))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }

    @Handles("subscriptions.command.activate-subscription")
    @FeatureControl(FEATURE_KEY)
    public void activate(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.activate-subscription")
                .build();

        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userAndGroupsService.getUserDetails(envelope)))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }

    @Handles("subscriptions.command.deactivate-subscription")
    @FeatureControl(FEATURE_KEY)
    public void deactivate(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.deactivate-subscription")
                .build();

        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userAndGroupsService.getUserDetails(envelope)))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }

    @Handles("subscriptions.command.delete-subscription")
    @FeatureControl(FEATURE_KEY)
    public void delete(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.delete-subscription")
                .build();

        final JsonObject payloadWithOrganisationId = createObjectBuilder(envelope.payloadAsJsonObject())
                .add(ORGANISATION_ID, extractOrganisationId(userAndGroupsService.getUserDetails(envelope)))
                .build();
        sender.send(envelopeFrom(metadata, payloadWithOrganisationId));
    }



    @Handles("subscriptions.command.create-subscription-by-user")
    @FeatureControl(FEATURE_KEY)
    public void subscriptionByUser(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("subscriptions.command.handler.create-subscription-by-user")
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
