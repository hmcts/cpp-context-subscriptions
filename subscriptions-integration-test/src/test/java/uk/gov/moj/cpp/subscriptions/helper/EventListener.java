package uk.gov.moj.cpp.subscriptions.helper;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;

public class EventListener {

    private final JmsMessageConsumerClient messageConsumerClient;

    public EventListener(final String eventName) {
        this.messageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(eventName).getMessageConsumerClient();
    }

    public String retrieveMessage() {
        final Optional<String> message = messageConsumerClient.retrieveMessage(2000L);
        assertThat(" message not found in public.event topic",
                message.isPresent(), is(true));
        return message.get();
    }
}
