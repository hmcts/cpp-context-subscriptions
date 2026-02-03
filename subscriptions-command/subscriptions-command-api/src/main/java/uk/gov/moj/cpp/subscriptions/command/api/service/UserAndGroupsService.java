package uk.gov.moj.cpp.subscriptions.command.api.service;

import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class UserAndGroupsService {

    private static final Logger LOGGER = getLogger(UserAndGroupsService.class);
    private static final String GET_USER_DETAILS = "usersgroups.get-logged-in-user-details";
    private static final String USER_ID = "userId";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public JsonObject getUserDetails(final JsonEnvelope envelope) {

        LOGGER.info("calling userAndGroups->getUserDetails with userId:{}", envelope.metadata().userId());

        final JsonObject payload = createObjectBuilder()
                .add(USER_ID, envelope.metadata().userId().orElseThrow(() -> new IllegalArgumentException("userId cannot be null!")))
                .build();

        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(payload)
                .withName(GET_USER_DETAILS).withMetadataFrom(envelope);

        final Envelope<JsonObject> jsonEnvelope = requester.request(
                requestEnvelope, JsonObject.class);

        final JsonObject result = jsonEnvelope.payload();
        LOGGER.info("from userAndGroups {}", result);
        return result;
    }

}
