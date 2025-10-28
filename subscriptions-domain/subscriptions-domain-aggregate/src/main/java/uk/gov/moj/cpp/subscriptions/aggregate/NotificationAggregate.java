package uk.gov.moj.cpp.subscriptions.aggregate;

import static java.util.Objects.isNull;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequestFailed.sendEmailRequestFailed;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequestSucceeded.sendEmailRequestSucceeded;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested.sendEmailRequested;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequestFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequestSucceeded;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;


public class NotificationAggregate implements Aggregate {
    private static final long serialVersionUID = 100L;
    private UUID subscriptionId;
    private String subscriptionName;
    private String sendToAddress;

    public Stream<Object> sendEmail(final UUID notificationId,
                                    final String sendToAddress, final String subject,
                                    final String body, final UUID subscriptionId,
                                    final String subscriptionName,
                                    final UUID templateId, final UUID materialId,
                                    final String title,
                                    final String caseLink
    ) {

        return apply(of(sendEmailRequested()
                .withNotificationId(notificationId)
                .withSendToAddress(sendToAddress)
                .withTitle(title)
                .withSubject(subject)
                .withBody(body)
                .withCaseLink(caseLink)
                .withSubscriptionId(subscriptionId)
                .withSubscriptionName(subscriptionName)
                .withTemplateId(templateId)
                .withMaterialId(materialId)
                .build()));
    }

    public Stream<Object> handleSendEmailSucceeded(final UUID notificationId, final ZonedDateTime sentTime) {
        if (isNull(subscriptionId)) {
            return empty();
        }
        return apply(of(sendEmailRequestSucceeded()
                .withNotificationId(notificationId)
                .withSentTime(sentTime)
                .withSendToAddress(this.sendToAddress)
                .withSubscriptionId(this.subscriptionId)
                .withSubscriptionName(this.subscriptionName)
                .build()));

    }

    public Stream<Object> handleSendEmailFailed(final UUID notificationId, final String errorMessage,
                                                final ZonedDateTime failedTime, final Integer statusCode) {
        if (isNull(subscriptionId)) {
            return empty();
        }
        return apply(of(sendEmailRequestFailed()
                .withNotificationId(notificationId)
                .withFailedTime(failedTime)
                .withErrorMessage(errorMessage)
                .withStatusCode(statusCode)
                .withSendToAddress(this.sendToAddress)
                .withSubscriptionId(this.subscriptionId)
                .withSubscriptionName(this.subscriptionName)
                .build()));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(SendEmailRequested.class).apply(e -> {
                    this.subscriptionId = e.getSubscriptionId();
                    this.subscriptionName = e.getSubscriptionName();
                    this.sendToAddress = e.getSendToAddress();
                }),
                when(SendEmailRequestSucceeded.class).apply(e -> {
                }),
                when(SendEmailRequestFailed.class).apply(e -> {
                })
        );
    }
}

