package uk.gov.moj.cpp.subscriptions.command.handler.helper;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.stream.Stream;

import javax.json.JsonValue;

public class CommandHandlerHelper {

    private CommandHandlerHelper() {
    }

    public static <T> void appendMetaDataInEventStream(final Envelope<T> envelope, final EventStream eventStream, final Stream<Object> newEvents) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(newEvents.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}
