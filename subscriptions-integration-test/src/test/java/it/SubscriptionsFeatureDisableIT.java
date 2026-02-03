package it;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import javax.json.Json;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.*;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.stubGetUserDetails;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.postMessageToTopicNotConsumed;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

@SuppressWarnings("squid:S2699")
class SubscriptionsFeatureDisableIT {

    private static String userId;
    private static String id;
    private static String courtId;
    private static String caseId;
    private static String caseUrn;
    private static String hearingId;


    @BeforeAll
    static void init() {
        final ImmutableMap<String, Boolean> features = of("subscriptionsPortal", false);
        FeatureStubber.stubFeaturesFor("subscriptions", features);
    }

    @BeforeEach
    void setUp() {
        id = randomUUID().toString();
        userId = randomUUID().toString();
        String organisationId = randomUUID().toString();
        courtId = randomUUID().toString();
        caseUrn = STRING.next();
        caseId = randomUUID().toString();
        hearingId = randomUUID().toString();
        stubGetUserDetails(userId, organisationId, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");
    }

    @Test
    void shouldNotAllowToCreateSubscriptionByAdmin() {
        final String payload = readFile("stub-data/create-subscription-command.json")
                .replace("%ID%", id);

        //when
        makePostCall(getWriteUrl("/subscriptions"),
                " application/vnd.subscriptions.command.create-subscription-by-admin+json",
                payload, userId, FORBIDDEN);

    }

    @Test
    void shouldNotAllowUserToDeactivateSubscription() {
        //given
        final String payload = Json.createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", id)),
                "application/vnd.subscriptions.command.deactivate-subscription+json",
                payload, userId, FORBIDDEN);
    }

    @Test
    void shouldNotAllowUserToActivateSubscription() {
        //given
        final String payload = Json.createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", id)),
                "application/vnd.subscriptions.command.activate-subscription+json",
                payload, userId, FORBIDDEN);
    }

    @Test
    void shouldNotAllowUserToDeleteSubscription() {
        //given
        final String payload = Json.createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", id)),
                "application/vnd.subscriptions.command.delete-subscription+json",
                payload, userId, FORBIDDEN);
    }

    @Test
    void shouldNotAllowUserToCreateSubscription() {
        //given
        final String payload = readFile("stub-data/create-subscription-by-user-command.json")
                .replace("%ID%", id);

        //when
        makePostCall(getWriteUrl("/subscriptions"),
                " application/vnd.subscriptions.command.create-subscription-by-user+json",
                payload, userId, FORBIDDEN);
    }

    @Test
    void shouldNotAllowUserToSubscribeSubscription() {
        //given
        final String payload = Json.createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", id)),
                "application/vnd.subscriptions.command.subscribe+json",
                payload, userId, FORBIDDEN);
    }

    @Test
    void shouldNotAllowUserToSubscribeUnsubscription() {
        //given
        final String payload = Json.createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", id)),
                "application/vnd.subscriptions.command.unsubscribe+json",
                payload, userId, FORBIDDEN);
    }

    @Test
    void shouldNotConsumeMessageFromHearingResulted() {
        final String payload = readFile("stub-data/public.hearing.resulted.json")
                .replace("%COURT_ID%", courtId)
                .replace("%CASE_ID%", caseId)
                .replace("%CASE_URN%", caseUrn);

        //when then
        postMessageToTopicNotConsumed(payload,
                "subscriptions.event.send-email-requested",
                "public.hearing.resulted");
    }

    @Test
    void shouldNotConsumeMessageFromNotificationSent() {
        final String notificationId = randomUUID().toString();
        final String payload = Json.createObjectBuilder()
                .add("notificationId", notificationId)
                .add("sentTime", "2021-05-09T08:31:40Z")
                .build().toString();

        //when then
        postMessageToTopicNotConsumed(payload,
                "subscriptions.event.send-email-request-succeeded",
                "public.notificationnotify.events.notification-sent");
    }

    @Test
    void shouldNotConsumeMessageFromNotificationFailed() {
        final String notificationId = randomUUID().toString();
        final String payload = Json.createObjectBuilder()
                .add("notificationId", notificationId)
                .add("failedTime", "2021-05-09T08:31:40Z")
                .add("statusCode", 500)
                .add("errorMessage", "errorMessage")
                .build().toString();

        //when then
        postMessageToTopicNotConsumed(payload,
                "subscriptions.event.send-email-request-failed",
                "public.notificationnotify.events.notification-failed");
    }


    @Test
    public void shouldNotProcessProgressionNowsDocumentRequested() {

        //given
        String MATERIAL_ID = randomUUID().toString();
        String REQUEST_ID = randomUUID().toString();
        String DEFENDANT_ID = randomUUID().toString();

        final String payload = readFile("stub-data/public.progression.now-document-requested.json")
                .replace("%HEARING_ID%", hearingId)
                .replaceAll("%MATERIAL_ID%", MATERIAL_ID)
                .replace("%REQUEST_ID%", REQUEST_ID)
                .replace("%DEFENDANT_ID%", DEFENDANT_ID)
                .replace("%CASE_URN%", caseUrn);

        //then

        postMessageToTopicNotConsumed(payload,
                "subscriptions.event.send-email-requested",
                "public.progression.now-document-requested");
    }

    @Test
    void shouldNotProcessHearingNowsNowDocumentRequested() {

        //given
        String MATERIAL_ID = randomUUID().toString();
        String REQUEST_ID = randomUUID().toString();
        String DEFENDANT_ID = randomUUID().toString();

        final String payload = readFile("stub-data/public.hearingnows.now-document-requested.json")
                .replace("%HEARING_ID%", hearingId)
                .replaceAll("%MATERIAL_ID%", MATERIAL_ID)
                .replace("%REQUEST_ID%", REQUEST_ID)
                .replace("%DEFENDANT_ID%", DEFENDANT_ID)
                .replace("%CASE_URN%", caseUrn);

        //then

        postMessageToTopicNotConsumed(payload,
                "subscriptions.event.send-email-requested",
                "public.hearingnows.now-document-requested");
    }

    @Test
    void shouldNotAllowGetSubscriptionsForAdmin() {
        poll(requestParams(getReadUrl("/subscriptions"),
                "application/vnd.subscriptions.query.subscriptions+json")
                .withHeader(HeaderConstants.USER_ID, userId))
                .timeout(60, SECONDS)
                .until(
                        status().is(FORBIDDEN));
    }

    @Test
    void shouldNotAllowGetSubscriptionsForUser() {
        poll(requestParams(getReadUrl("/subscriptions"),
                "application/vnd.subscriptions.query.subscriptions-by-user+json")
                .withHeader(HeaderConstants.USER_ID, userId))
                .timeout(60, SECONDS)
                .until(
                        status().is(FORBIDDEN));
    }


    @Test
    void shouldNotAllowDeleteSubscriberForUser() {
        final String payload = Json.createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", id)),
                "application/vnd.subscriptions.command.delete-subscriber+json",
                payload, userId, FORBIDDEN);


    }
}
