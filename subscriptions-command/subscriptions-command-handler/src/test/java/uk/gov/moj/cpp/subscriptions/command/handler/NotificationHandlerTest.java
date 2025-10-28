package uk.gov.moj.cpp.subscriptions.command.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.HandleSendEmailRequestFailed.handleSendEmailRequestFailed;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.HandleSendEmailRequestSucceeded.handleSendEmailRequestSucceeded;
import static uk.gov.moj.cpp.subscriptions.json.schemas.handler.SendEmail.sendEmail;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.aggregate.NotificationAggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.HandleSendEmailRequestFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.HandleSendEmailRequestSucceeded;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.SendEmail;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationHandlerTest {

    @Mock
    private NotificationAggregate notificationAggregate;

    @Mock
    private Stream<Object> newEvents;

    @Mock
    private Stream<Object> mappedNewEvents;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptorStream;

    @InjectMocks
    private NotificationHandler notificationHandler;

    @Test
    public void shouldHandleSendEmail() throws EventStreamException {
        //given
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, NotificationAggregate.class)).thenReturn(notificationAggregate);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        final SendEmail sendEmail = sendEmail()
                .withSubscriptionId(randomUUID())
                .withSubscriptionName("Subscription Name")
                .withTitle("Defendant Present")
                .withSubject("subject")
                .withBody("body")
                .withCaseLink("Access the case dummyUrl/case-at-a-glance/c6142858-16d1-43ed-afbc-3fb67682f2b0 for full details.")
                .withSendToAddress("abc@xyz.com")
                .withTemplateId(randomUUID())
                .withMaterialId(randomUUID())
                .build();

        when(notificationAggregate.sendEmail(any(), eq(sendEmail.getSendToAddress()),
                eq(sendEmail.getSubject()),
                eq(sendEmail.getBody()),
                eq(sendEmail.getSubscriptionId()),
                eq(sendEmail.getSubscriptionName()),
                eq(sendEmail.getTemplateId()),
                eq(sendEmail.getMaterialId()),
                eq(sendEmail.getTitle()),
                eq(sendEmail.getCaseLink())

        ))
                .thenReturn(newEvents);

        //when
        notificationHandler.handleSendEmail(envelopeFrom(
                metadataWithRandomUUID("subscriptions.command.handler.send-email"),
                sendEmail));

        //then
        verify(eventStream).append(argumentCaptorStream.capture());
        assertThat(argumentCaptorStream.getValue(), is(mappedNewEvents));
    }

    @Test
    public void shouldHandleSendEmailSucceeded() throws EventStreamException {
        //given
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, NotificationAggregate.class)).thenReturn(notificationAggregate);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        final HandleSendEmailRequestSucceeded handleSendEmailRequestSucceeded = handleSendEmailRequestSucceeded()
                .withNotificationId(randomUUID())
                .withSentTime(ZonedDateTime.now())
                .build();

        when(notificationAggregate.handleSendEmailSucceeded(
                handleSendEmailRequestSucceeded.getNotificationId(),
                handleSendEmailRequestSucceeded.getSentTime()))
                .thenReturn(newEvents);

        //when
        notificationHandler.handleEmailSentSucceeded(envelopeFrom(
                metadataWithRandomUUID("subscriptions.command.handler.handle-email-sent-succeeded"),
                handleSendEmailRequestSucceeded));

        //then
        verify(eventStream).append(argumentCaptorStream.capture());
        assertThat(argumentCaptorStream.getValue(), is(mappedNewEvents));
    }

    @Test
    public void shouldHandleSendEmailFailed() throws EventStreamException {
        //given
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, NotificationAggregate.class)).thenReturn(notificationAggregate);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        final HandleSendEmailRequestFailed handleSendEmailRequestFailed = handleSendEmailRequestFailed()
                .withNotificationId(randomUUID())
                .withFailedTime(ZonedDateTime.now())
                .withStatusCode(500)
                .withErrorMessage("error message")
                .build();

        when(notificationAggregate.handleSendEmailFailed(
                handleSendEmailRequestFailed.getNotificationId(),
                handleSendEmailRequestFailed.getErrorMessage(),
                handleSendEmailRequestFailed.getFailedTime(),
                handleSendEmailRequestFailed.getStatusCode()))
                .thenReturn(newEvents);

        //when
        notificationHandler.handleEmailSentFailed(envelopeFrom(
                metadataWithRandomUUID("subscriptions.command.handler.handle-email-sent-failed"),
                handleSendEmailRequestFailed));

        //then
        verify(eventStream).append(argumentCaptorStream.capture());
        assertThat(argumentCaptorStream.getValue(), is(mappedNewEvents));
    }
}
