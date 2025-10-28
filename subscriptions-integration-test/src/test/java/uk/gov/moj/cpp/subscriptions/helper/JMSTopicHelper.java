package uk.gov.moj.cpp.subscriptions.helper;


import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.UUID;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

public class JMSTopicHelper {

    private static final String USER_ID = UUID.randomUUID().toString();

    public void sendMessageToPublicTopic(final String commandName, final JsonObject payload) {
        final Metadata metadata = createMetadataForCommandWith(commandName);

        sendMessageToPublicTopic(commandName, payload, metadata);
    }

    private Metadata createMetadataForCommandWith(final String commandName) {

        return Envelope.metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(commandName)
                .createdAt(ZonedDateTime.now())
                .withUserId(USER_ID)
                .withClientCorrelationId(UUID.randomUUID().toString())
                .withSource(RandomStringUtils.randomAlphanumeric(10))
                .build();
    }

    private void sendMessageToPublicTopic(final String commandName, final JsonObject payload, final Metadata metadata) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final JmsMessageProducerClient jmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();
        jmsMessageProducerClient.sendMessage(commandName, jsonEnvelope);
    }
}
