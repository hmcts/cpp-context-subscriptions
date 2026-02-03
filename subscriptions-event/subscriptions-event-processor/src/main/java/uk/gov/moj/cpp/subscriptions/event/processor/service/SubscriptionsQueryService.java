package uk.gov.moj.cpp.subscriptions.event.processor.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SubscriptionsQueryService {
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);


    public List<Subscription> findSubscriptionsByCourt(UUID courtId, Requester requester) {
        final JsonObject subscriptionsByCourtId = getSubscriptionsByCourtId(courtId, requester);

        final CourtSubscriptions courtSubscriptions = jsonObjectToObjectConverter.convert(subscriptionsByCourtId, CourtSubscriptions.class);
        return courtSubscriptions.getSubscriptions();
    }

    public JsonObject getSubscriptionsByCourtId(final UUID courtId, Requester requester) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("subscriptions.query.subscriptions.by-court-id").build();

        final JsonObject payload = createObjectBuilder()
                .add("courtId", courtId.toString())
                .build();

        final Envelope<JsonObject> jsonEnvelope = requester.requestAsAdmin(
                JsonEnvelope.envelopeFrom(metadata, payload), JsonObject.class);

        return jsonEnvelope.payload();
    }

    public static class CourtSubscriptions {
        private List<Subscription> subscriptions;

        public List<Subscription> getSubscriptions() {
            return subscriptions;
        }
    }
}
