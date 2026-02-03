package uk.gov.moj.cpp.subscriptions.event.processor.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class EmailInfoSender {

    private static final String SUBSCRIPTIONS_COMMAND_HANDLER_SEND_EMAIL = "subscriptions.command.handler.send-email";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public void sendCommand(final Envelope<?> event, final List<EmailInfo> emailInfos) {
        emailInfos.forEach(emailInfo ->
                emailInfo.getSubscription().getSubscribers()
                        .stream()
                        .filter(Subscribers::getActive)
                        .forEach(subscriber ->
                                sender.send(envelopeFrom(metadataFrom(event.metadata())
                                                        .withName(SUBSCRIPTIONS_COMMAND_HANDLER_SEND_EMAIL)
                                                        .build(),
                                                createPayload(emailInfo, subscriber)
                                        )
                                )));
    }

    private JsonObject createPayload(final EmailInfo emailInfo, final Subscribers subscriber) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("sendToAddress", subscriber.getEmailAddress())
                .add("subject", emailInfo.getSubject())
                .add("title", emailInfo.getTitle())
                .add("body", emailInfo.getBody())
                .add("caseLink", emailInfo.getCaseLink())
                .add("subscriptionId", emailInfo.getSubscription().getId().toString())
                .add("subscriptionName", emailInfo.getSubscription().getName())
                .add("templateId", emailInfo.getEmailTemplateId());


        if (isNotEmpty(emailInfo.getMaterialId())) {
            payloadBuilder.add("materialId", emailInfo.getMaterialId());
        }
        return payloadBuilder.build();
    }
}
