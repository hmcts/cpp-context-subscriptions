package uk.gov.moj.cpp.subscriptions.event.processor;


import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class NotificationEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventProcessor.class);

    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_SUCCEEDED = "subscriptions.command.handler.handle-send-email-request-succeeded";
    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_FAILED = "subscriptions.command.handler.handle-send-email-request-failed";

    private static final String NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE = "notificationnotify.send-email-notification";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String PERSONALISATION = "personalisation";
    private static final String SUBJECT = "subject";
    private static final String BODY = "body";
    private static final String TITLE = "title";
    private static final String CASE_LINK = "caseLink";
    private static final String STATUS_CODE = "statusCode";
    private static final String MATERIAL_URL = "materialUrl";

    @Inject
    private Sender sender;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("subscriptions.event.send-email-requested")
    public void processSendEmailRequested(final Envelope<SendEmailRequested> event) {
        final SendEmailRequested payload = event.payload();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subscriptions.event.send-email-requested {}", payload);
        }

        JsonObjectBuilder emailNotificationBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, payload.getNotificationId().toString())
                .add(TEMPLATE_ID, payload.getTemplateId().toString())
                .add(SEND_TO_ADDRESS, payload.getSendToAddress());


        JsonObjectBuilder personalisationBuilder = createObjectBuilder()
                .add(SUBJECT, payload.getSubject())
                .add(BODY, payload.getBody());

        if (payload.getTitle() != null) {
            personalisationBuilder.add(TITLE, payload.getTitle());
        }

        if (payload.getCaseLink() != null) {
            personalisationBuilder.add(CASE_LINK, payload.getCaseLink());
        }

        emailNotificationBuilder.add(PERSONALISATION, personalisationBuilder);


        if (nonNull(payload.getMaterialId())) {
            emailNotificationBuilder.add(MATERIAL_URL, applicationParameters.getMaterialUrl(payload.getMaterialId()));
        }

        final Envelope<JsonObject> jsonEnvelope = envelopeFrom(event.metadata(), emailNotificationBuilder.build());
        sendEmailNotification(jsonEnvelope);
    }

    private void sendEmailNotification(final Envelope<JsonObject> envelope) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending email notification - {} ", envelope.payload());
        }

        sender.sendAsAdmin(
                envelopeFrom(
                        metadataFrom(envelope.metadata()).withName(NOTIFICATION_NOTIFY_EMAIL_METADATA_TYPE),
                        envelope.payload()
                )
        );
    }

    @Handles("public.notificationnotify.events.notification-sent")
    public void processNotificationSucceeded(final JsonEnvelope event) {
        if (featureControlGuard.isFeatureEnabled("subscriptionsPortal")) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("public.notificationnotify.events.notification-sent {}", event.payload());
            }
            final JsonObject payload = event.payloadAsJsonObject();
            final String notificationId = payload.getString(NOTIFICATION_ID);
            final String sentTime = payload.getString("sentTime");

            final JsonObject notificationSentPayload = createObjectBuilder()
                    .add(NOTIFICATION_ID, notificationId)
                    .add("sentTime", sentTime)
                    .build();

            sender.send(envelopeFrom(metadataFrom(event.metadata())
                            .withName(SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_SUCCEEDED)
                            .build(),
                    notificationSentPayload));
        } else {
            LOGGER.info("'subscriptionsPortal' disabled so not processing 'public.notificationnotify.events.notification-sent' event");
        }
    }

    @Handles("public.notificationnotify.events.notification-failed")
    public void processNotificationFailed(final JsonEnvelope event) {
        if (featureControlGuard.isFeatureEnabled("subscriptionsPortal")) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("public.notificationnotify.events.notification-failed {}", event.payload());
            }

            final JsonObject notificationFailedPayload = event.payloadAsJsonObject();
            final String notificationId = notificationFailedPayload.getString(NOTIFICATION_ID);
            final String failedTime = notificationFailedPayload.getString("failedTime");
            final String errorMessage = notificationFailedPayload.getString("errorMessage");

            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add(NOTIFICATION_ID, notificationId)
                    .add("failedTime", failedTime)
                    .add("errorMessage", errorMessage);

            if (notificationFailedPayload.containsKey(STATUS_CODE)) {
                final int statusCode = notificationFailedPayload.getInt(STATUS_CODE);
                jsonObjectBuilder.add(STATUS_CODE, statusCode);
            }

            sender.send(envelopeFrom(metadataFrom(event.metadata())
                            .withName(SUBSCRIPTIONS_COMMAND_HANDLER_HANDLE_SEND_EMAIL_REQUEST_FAILED)
                            .build(),
                    jsonObjectBuilder.build()));

        } else {
            LOGGER.info("'subscriptionsPortal' disabled so not processing 'public.notificationnotify.events.notification-failed' event");
        }
    }
}
