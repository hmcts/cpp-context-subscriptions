package uk.gov.moj.cpp.subscriptions.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequestFailed;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequestSucceeded;
import uk.gov.moj.cpp.subscriptions.json.schemas.SendEmailRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationAggregateTest {

    @InjectMocks
    private NotificationAggregate notificationAggregate;

    @Test
    public void shouldSendEmailRequest() {
        final UUID subscriptionId = randomUUID();
        final UUID notificationId = randomUUID();
        final UUID templateId = randomUUID();
        final UUID materialId = randomUUID();
        final Stream<Object> eventStream = notificationAggregate.sendEmail(notificationId, "abc@xyz", "subject", "body",
                subscriptionId, "subscription name", templateId, materialId, "Trial Effectiveness", "case link");
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SendEmailRequested.class));
        SendEmailRequested sendEmailRequested = (SendEmailRequested) eventList.get(0);
        assertThat(sendEmailRequested.getNotificationId(), is(notificationId));
        assertThat(sendEmailRequested.getSubject(), is("subject"));
        assertThat(sendEmailRequested.getBody(), is("body"));
        assertThat(sendEmailRequested.getSendToAddress(), is("abc@xyz"));
        assertThat(sendEmailRequested.getSubscriptionName(), is("subscription name"));
        assertThat(sendEmailRequested.getSubscriptionId(), is(subscriptionId));
        assertThat(sendEmailRequested.getTemplateId(), is(templateId));
        assertThat(sendEmailRequested.getMaterialId(), is(materialId));
        assertThat(sendEmailRequested.getTitle(), is("Trial Effectiveness"));
        assertThat(sendEmailRequested.getCaseLink(), is("case link"));
    }

    @Test
    public void shouldHandleSendEmailRequestSucceeded() {
        final UUID notificationId = randomUUID();
        final UUID subscriptionId = randomUUID();
        final ZonedDateTime sentTime = ZonedDateTime.now();
        notificationAggregate.sendEmail(notificationId, "abc@xyz.com", "subject", "body", subscriptionId, "subscription name", null, null, null, null);

        final Stream<Object> eventStream = notificationAggregate.handleSendEmailSucceeded(
                notificationId,
                sentTime);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SendEmailRequestSucceeded.class));
        SendEmailRequestSucceeded sendEmailRequestSucceeded = (SendEmailRequestSucceeded) eventList.get(0);
        assertThat(sendEmailRequestSucceeded.getNotificationId(), is(notificationId));
        assertThat(sendEmailRequestSucceeded.getSentTime(), is(sentTime));
        assertThat(sendEmailRequestSucceeded.getSubscriptionId(), is(subscriptionId));
        assertThat(sendEmailRequestSucceeded.getSubscriptionName(), is("subscription name"));
        assertThat(sendEmailRequestSucceeded.getSendToAddress(), is("abc@xyz.com"));
    }

    @Test
    public void shouldNotHandleSendEmailRequestSucceededWhenNotSentBefore() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime sentTime = ZonedDateTime.now();

        final Stream<Object> eventStream = notificationAggregate.handleSendEmailSucceeded(
                notificationId,
                sentTime);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList, empty());
    }

    @Test
    public void shouldHandleSendEmailRequestFailed() {
        final UUID notificationId = randomUUID();
        final UUID subscriptionId = randomUUID();
        final ZonedDateTime failedTime = ZonedDateTime.now();
        notificationAggregate.sendEmail(notificationId, "abc@xyz.com", "subject", "body", subscriptionId, "subscription name", null, null, "", "");

        final Stream<Object> eventStream = notificationAggregate.handleSendEmailFailed(
                notificationId,
                "error message",
                failedTime,
                500);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(SendEmailRequestFailed.class));
        SendEmailRequestFailed sendEmailRequestFailed = (SendEmailRequestFailed) eventList.get(0);
        assertThat(sendEmailRequestFailed.getNotificationId(), is(notificationId));
        assertThat(sendEmailRequestFailed.getErrorMessage(), is("error message"));
        assertThat(sendEmailRequestFailed.getStatusCode(), is(500));
        assertThat(sendEmailRequestFailed.getFailedTime(), is(failedTime));
        assertThat(sendEmailRequestFailed.getSubscriptionId(), is(subscriptionId));
        assertThat(sendEmailRequestFailed.getSubscriptionName(), is("subscription name"));
        assertThat(sendEmailRequestFailed.getSendToAddress(), is("abc@xyz.com"));

    }

    @Test
    public void shouldNotHandleSendEmailRequestFailedWhenNotSentBefore() {
        final UUID notificationId = randomUUID();
        final ZonedDateTime failedTime = ZonedDateTime.now();

        final Stream<Object> eventStream = notificationAggregate.handleSendEmailFailed(
                notificationId,
                "error message",
                failedTime,
                500);
        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList, empty());
    }
}
