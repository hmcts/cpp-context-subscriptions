package uk.gov.moj.cpp.subscriptions.event.processor.service;


import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EmailInfoSenderTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private EmailInfoSender emailInfoSender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Test
    public void shouldSendCommand() {

        final Envelope<NowDocumentRequested> nowDocumentRequestedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                null);

        final List<EmailInfo> emailInfos = asList(
                emailInfo()
                        .withSubscription(subscription()
                                .withId(randomUUID())
                                .withName("subscription name")
                                .withSubscribers(asList(subscribers()
                                        .withEmailAddress("abc@xyz.com")
                                        .withActive(true)
                                        .build()))
                                .build())
                        .withSubject("Subject")
                        .withBody("Body")
                        .withTitle("title")
                        .withCaseLink("caseLink")
                        .withEmailTemplateId(randomUUID().toString())
                        .build());

        emailInfoSender.sendCommand(nowDocumentRequestedEnvelope, emailInfos);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), equalTo("subscriptions.command.handler.send-email"));
        assertThat(publicEvent.payload().getString("subject"), is("Subject"));
    }
}
