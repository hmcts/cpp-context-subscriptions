package uk.gov.moj.cpp.subscriptions.event.processor;

import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;

@SuppressWarnings({"squid:S00112"})
public class ProcessorHelper {
    private ProcessorHelper() {

    }

    public static <T> void sendPublicEvent(final String metadataName, final Metadata envelopeMetadata,
                                           final T payload, final Sender sender) {

        final MetadataBuilder builder = metadataFrom(envelopeMetadata);

        final Metadata metadata = builder
                .withName(metadataName)
                .build();
        sender.send(envelopeFrom(metadata, payload));
    }
}
