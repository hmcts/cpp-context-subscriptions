package uk.gov.moj.cpp.subscriptions.event.processor;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested.sendEmailRequested;

import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationEventProcessorTest {

    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_SUCCEEDED = "subscriptions.command.handler.handle-send-email-request-succeeded";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_FAILED = "subscriptions.command.handler.handle-send-email-request-failed";
    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";

    @Mock
    private Sender sender;

    @InjectMocks
    private NotificationEventProcessor notificationEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Test
    public void shouldProcessSendEmailRequestedWithoutMandatoryFields() {

        final UUID notificationId = randomUUID();
        final SendEmailRequested sendEmailRequested = sendEmailRequested()
                .withNotificationId(notificationId)
                .withSubject("subject")
                .withBody("body")

                .withSendToAddress("abc@xyz.com")
                .withTemplateId(randomUUID())
                .withMaterialId(randomUUID())
                .build();

        final Envelope<SendEmailRequested> hearingResultedEnvelope = Envelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                sendEmailRequested);

        when(applicationParameters.getMaterialUrl(sendEmailRequested.getMaterialId())).thenReturn("http://materialUrl");
        notificationEventProcessor.processSendEmailRequested(hearingResultedEnvelope);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), equalTo(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE));
        assertThat(envelopeCaptor.getValue().payload().getString("notificationId"), equalTo(notificationId.toString()));
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("personalisation").getString("subject"), equalTo("subject"));
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("personalisation").getString("body"), equalTo("body"));
        assertThat(envelopeCaptor.getValue().payload().getString("sendToAddress"), equalTo("abc@xyz.com"));
        assertThat(envelopeCaptor.getValue().payload().getString("templateId"), equalTo(sendEmailRequested.getTemplateId().toString()));
        assertThat(envelopeCaptor.getValue().payload().getString("materialUrl"), equalTo(format("http://materialUrl", sendEmailRequested.getMaterialId())));
    }
    @Test
    public void shouldProcessSendEmailRequested() {

        final UUID notificationId = randomUUID();
        final SendEmailRequested sendEmailRequested = sendEmailRequested()
                .withNotificationId(notificationId)
                .withSubject("subject")
                .withBody("body")
                .withTitle("Defendant Present")
                .withCaseLink("http://localhost:8080/prosecution-casefile/case-at-a-glance")
                .withSendToAddress("abc@xyz.com")
                .withTemplateId(randomUUID())
                .withMaterialId(randomUUID())
                .build();

        final Envelope<SendEmailRequested> hearingResultedEnvelope = Envelope.envelopeFrom(metadataWithRandomUUIDAndName().build(),
                sendEmailRequested);

        when(applicationParameters.getMaterialUrl(sendEmailRequested.getMaterialId())).thenReturn("http://materialUrl");
        notificationEventProcessor.processSendEmailRequested(hearingResultedEnvelope);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), equalTo(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE));
        assertThat(envelopeCaptor.getValue().payload().getString("notificationId"), equalTo(notificationId.toString()));
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("personalisation").getString("subject"), equalTo("subject"));
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("personalisation").getString("body"), equalTo("body"));
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("personalisation").getString("title"), equalTo("Defendant Present"));
        assertThat(envelopeCaptor.getValue().payload().getJsonObject("personalisation").getString("caseLink"), equalTo("http://localhost:8080/prosecution-casefile/case-at-a-glance"));
        assertThat(envelopeCaptor.getValue().payload().getString("sendToAddress"), equalTo("abc@xyz.com"));
        assertThat(envelopeCaptor.getValue().payload().getString("templateId"), equalTo(sendEmailRequested.getTemplateId().toString()));
        assertThat(envelopeCaptor.getValue().payload().getString("materialUrl"), equalTo(format("http://materialUrl", sendEmailRequested.getMaterialId())));
    }

    @Test
    public void shouldProcessNotificationSucceeded() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(true);
        final String notificationId = randomUUID().toString();
        final String sentTime = LocalDate.now().toString();
        final JsonObject notificationSentPayload = createObjectBuilder()
                .add("notificationId", notificationId)
                .add("sentTime", sentTime)
                .build();

        final JsonEnvelope notificationSucceededPayload = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                notificationSentPayload);

        notificationEventProcessor.processNotificationSucceeded(notificationSucceededPayload);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), equalTo(SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_SUCCEEDED));
        assertThat(envelopeCaptor.getValue().payload().getString("notificationId"), equalTo(notificationId));
        assertThat(envelopeCaptor.getValue().payload().getString("sentTime"), equalTo(sentTime));
    }

    @Test
    public void shouldProcessNotificationFailed() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(true);

        final String notificationId = randomUUID().toString();
        final String failedTime = LocalDate.now().toString();
        final String errorMessage = "an error message";
        final JsonObject notificationFailedPayload = createObjectBuilder()
                .add("notificationId", notificationId)
                .add("failedTime", failedTime)
                .add("errorMessage", errorMessage)
                .build();

        final JsonEnvelope notificationSucceededPayload = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                notificationFailedPayload);

        notificationEventProcessor.processNotificationFailed(notificationSucceededPayload);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), equalTo(SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_FAILED));
        assertThat(envelopeCaptor.getValue().payload().getString("notificationId"), equalTo(notificationId));
        assertThat(envelopeCaptor.getValue().payload().getString("failedTime"), equalTo(failedTime));
        assertThat(envelopeCaptor.getValue().payload().getString("errorMessage"), equalTo(errorMessage));
    }
}
