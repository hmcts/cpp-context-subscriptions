package uk.gov.moj.cpp.subscriptions.event.processor.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

public class HearingService {

    private static final Logger LOGGER = getLogger(HearingService.class);

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    private static final String HEARING_GET_HEARING = "hearing.get.hearing";

    public Optional<Hearing> getHearing(final UUID hearingId) {

        LOGGER.info("calling hearing.get.hearing->getHearing with hearingId:{}", hearingId);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(HEARING_GET_HEARING).build();

        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();

        final Envelope<JsonObject> responseAsJsonObject = requester.requestAsAdmin(
                envelopeFrom(metadata, payload), JsonObject.class);

        final JsonObject payloadAsJsonObject = responseAsJsonObject.payload();
        if (payloadAsJsonObject.isEmpty()) {
            return Optional.empty();
        }

        final Hearing result = jsonObjectToObjectConverter.convert(payloadAsJsonObject.getJsonObject("hearing"), Hearing.class);
        LOGGER.info("from hearing.get.hearing {}", result);

        return of(result);
    }
}
