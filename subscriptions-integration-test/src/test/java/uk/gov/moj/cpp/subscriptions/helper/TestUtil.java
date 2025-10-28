package uk.gov.moj.cpp.subscriptions.helper;

import com.google.common.io.Resources;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.nio.charset.Charset;
import java.util.Optional;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static it.BaseIT.CONTEXT_NAME;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;

public class TestUtil {

    public static String readFile(String filePath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(filePath),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            fail("Error consuming file from location " + filePath);
        }
        return request;
    }

    public static String postMessageToTopicAndVerify(final String payload, final String eventName, final String commandName,
                                                     final boolean verify, final Matcher<? super ReadContext>... matchers) {
        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        final JmsMessageConsumerClient caseManagement = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(eventName).getMessageConsumerClient();

        final JMSTopicHelper publicTopicHelper = new JMSTopicHelper();
        publicTopicHelper.sendMessageToPublicTopic(commandName, stringToJsonObjectConverter.convert(payload));
        if (verify) {
            Optional<String> message = caseManagement.retrieveMessage(10000L);
            assertThat(eventName + " message not found in subscriptions.event topic", message.isPresent(), is(true));
            return message.get();
        }

        return null;
    }

    public static void postMessageToTopicNotConsumed(final String payload, final String eventName, final String commandName) {
        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        final JmsMessageConsumerClient subscriptions = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(eventName).getMessageConsumerClient();

        final JMSTopicHelper publicTopicHelper = new JMSTopicHelper();
        publicTopicHelper.sendMessageToPublicTopic(commandName, stringToJsonObjectConverter.convert(payload));
        Optional<String> message = subscriptions.retrieveMessage(800L);
        assertThat(eventName + " message not found in subscriptions.event topic", message.isPresent(), is(false));
    }
}
