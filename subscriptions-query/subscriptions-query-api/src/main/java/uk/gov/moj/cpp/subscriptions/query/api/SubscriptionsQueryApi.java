package uk.gov.moj.cpp.subscriptions.query.api;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.FeatureControl;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.query.SubscriptionsQueryView;
import uk.gov.moj.cpp.subscriptions.query.api.service.UserAndGroupsService;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class SubscriptionsQueryApi {

    private static final String FEATURE_KEY = "subscriptionsPortal";
    public static final String ORGANISATION_ID = "organisationId";
    @Inject
    private SubscriptionsQueryView subscriptionsQueryView;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    @Handles("subscriptions.query.subscriptions")
    @FeatureControl(FEATURE_KEY)
    public JsonEnvelope retrieveSubscriptions(final JsonEnvelope query) {
        final JsonObject userDetails = userAndGroupsService.getUserDetails(query);
        return this.subscriptionsQueryView.retrieveSubscriptions(query, fromString(extractOrganisationId(userDetails)));
    }

    @Handles("subscriptions.query.subscriptions.by-court-id")
    @FeatureControl(FEATURE_KEY)
    public JsonEnvelope retrieveSubscriptionsByCourtId(final JsonEnvelope query) {
        return this.subscriptionsQueryView.retrieveSubscriptionsByCourtId(query, fromString(query.payloadAsJsonObject().getString("courtId")));
    }
    @Handles("subscriptions.query.subscriptions-by-user")
    @FeatureControl(FEATURE_KEY)
    public JsonEnvelope retrieveSubscriptionsByUser(final JsonEnvelope query) {
        final JsonObject userDetails = userAndGroupsService.getUserDetails(query);
        return this.subscriptionsQueryView.retrieveSubscriptions(query, fromString(extractOrganisationId(userDetails)), userDetails.getString("email"));
    }

    private String extractOrganisationId(final JsonObject response) {
        if(response.containsKey(ORGANISATION_ID) && nonNull(response.getString(ORGANISATION_ID))) {
            return response.getString(ORGANISATION_ID);
        }
        throw new BadRequestException("User does not belong to any organisation");
    }
}
