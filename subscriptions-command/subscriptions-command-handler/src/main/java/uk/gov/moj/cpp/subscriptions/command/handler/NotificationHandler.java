package uk.gov.moj.cpp.subscriptions.command.handler;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.subscriptions.command.handler.helper.CommandHandlerHelper.appendMetaDataInEventStream;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.aggregate.NotificationAggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.HandleSendEmailRequestFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.HandleSendEmailRequestSucceeded;
import uk.gov.moj.cpp.subscriptions.json.schemas.handler.SendEmail;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class NotificationHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("subscriptions.command.handler.send-email")
    public void handleSendEmail(final Envelope<SendEmail> envelope) throws EventStreamException {
        final SendEmail sendEmail = envelope.payload();
        final UUID notificationId = randomUUID();
        final EventStream eventStream = eventSource.getStreamById(notificationId);
        final NotificationAggregate notificationAggregate = aggregateService.get(eventStream, NotificationAggregate.class);
        final Stream<Object> events = notificationAggregate.sendEmail(notificationId,sendEmail.getSendToAddress(),
                sendEmail.getSubject(), sendEmail.getBody(), sendEmail.getSubscriptionId(),
                sendEmail.getSubscriptionName(),
                sendEmail.getTemplateId(),
                sendEmail.getMaterialId(),
                sendEmail.getTitle(),
                sendEmail.getCaseLink()

        );

        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.handle-send-email-request-succeeded")
    public void handleEmailSentSucceeded(final Envelope<HandleSendEmailRequestSucceeded> envelope) throws EventStreamException {

        final HandleSendEmailRequestSucceeded handleSendEmailRequestSucceeded = envelope.payload();

        final UUID notificationId = handleSendEmailRequestSucceeded.getNotificationId();
        final ZonedDateTime sentTime = handleSendEmailRequestSucceeded.getSentTime();
        final EventStream eventStream = eventSource.getStreamById(handleSendEmailRequestSucceeded.getNotificationId());
        final NotificationAggregate notificationAggregate = aggregateService.get(eventStream, NotificationAggregate.class);

        final Stream<Object> events = notificationAggregate.handleSendEmailSucceeded(notificationId, sentTime);

        appendMetaDataInEventStream(envelope, eventStream, events);
    }

    @Handles("subscriptions.command.handler.handle-send-email-request-failed")
    public void handleEmailSentFailed(final Envelope<HandleSendEmailRequestFailed> envelope) throws EventStreamException {

        final HandleSendEmailRequestFailed handleSendEmailRequestFailed = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(handleSendEmailRequestFailed.getNotificationId());
        final NotificationAggregate notificationAggregate = aggregateService.get(eventStream, NotificationAggregate.class);

        final Stream<Object> events = notificationAggregate.handleSendEmailFailed(handleSendEmailRequestFailed.getNotificationId(),
                handleSendEmailRequestFailed.getErrorMessage(), handleSendEmailRequestFailed.getFailedTime(),
                handleSendEmailRequestFailed.getStatusCode());

        appendMetaDataInEventStream(envelope, eventStream, events);
    }

}
