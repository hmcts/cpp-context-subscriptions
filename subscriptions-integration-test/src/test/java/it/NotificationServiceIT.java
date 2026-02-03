package it;


import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.stubNotificationService;
import static uk.gov.moj.cpp.subscriptions.helper.RegexMatcher.matchesRegex;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getReadUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getWriteUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.makePostCall;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.stubGetUserDetails;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NotificationServiceIT {

    private static final String USER_ID = randomUUID().toString();
    private static String SUBSCRIPTION_ID = randomUUID().toString();
    private static String ORGANISATION_ID = randomUUID().toString();
    private static String COURT_ID = randomUUID().toString();
    private static String CASE_ID = randomUUID().toString();
    private static String CASE_URN =STRING.next();
    private static String NOTIFICATION_ID;

    @BeforeAll
    public static void init() {
        resetStubs();
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");
        stubNotificationService();
        final ImmutableMap<String, Boolean> features = of("subscriptionsPortal", true);
        FeatureStubber.stubFeaturesFor("subscriptions", features);
        NOTIFICATION_ID = initiateNotification();
    }

    @Test
    public void shouldRaiseSendEmailRequestSucceededPrivateEvent() {

        final String payload = createObjectBuilder()
                .add("notificationId", NOTIFICATION_ID)
                .add("sentTime", "2021-05-09T08:31:40Z")
                .build().toString();

        //when then
        postMessageToTopicAndVerify(payload,
                "subscriptions.event.send-email-request-succeeded",
                "public.notificationnotify.events.notification-sent", true,
                withJsonPath("$.notificationId", equalTo(NOTIFICATION_ID)),
                withJsonPath("$.sentTime", startsWith("2021-05-09T08:31"))
        );
    }

    @Test
    public void shouldRaiseSendEmailRequestFailedPrivateEvent() {

        final String payload = createObjectBuilder()
                .add("notificationId", NOTIFICATION_ID)
                .add("failedTime", "2021-05-09T08:31:40Z")
                .add("statusCode", 500)
                .add("errorMessage", "errorMessage")
                .build().toString();

        //when then
        postMessageToTopicAndVerify(payload,
                "subscriptions.event.send-email-request-failed",
                "public.notificationnotify.events.notification-failed", true,
                withJsonPath("$.notificationId", equalTo(NOTIFICATION_ID)),
                withJsonPath("$.statusCode", equalTo(500)),
                withJsonPath("$.errorMessage", equalTo("errorMessage")),
                withJsonPath("$.failedTime", startsWith("2021-05-09T08:31"))
        );
    }


    public static String initiateNotification() {

        //given
        createSubscription();

        final String payload = readFile("stub-data/public.hearing.resulted.json")
                .replace("%COURT_ID%", COURT_ID)
                .replace("%CASE_ID%", CASE_ID)
                .replace("%CASE_URN%", CASE_URN);

        //when then
        final String body = MessageFormat.format("<h2>Plea entered</h2><p>Robert ORMSBY - 17 January 1968{0}Occupy reserved seat / berth without a valid ticket on the Tyne and Wear Metro{0}GUILTY</p><a _target=\"blank\" href=\"(.*)prosecution-casefile/case-at-a-glance/{1}\">Access the case </a>for full details.", "<br>", CASE_ID);
        final String subject = format("Case %s - plea entered", CASE_URN);

        final String eventPayload = postMessageToTopicAndVerify(payload,
                "subscriptions.event.send-email-requested",
                "public.hearing.resulted", true,
                withJsonPath("$.subject", equalTo(subject)),
                withJsonPath("$.body", matchesRegex(body)));

        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        return stringToJsonObjectConverter.convert(eventPayload).getString("notificationId");
    }

    private static void createSubscription() {
        //given
        final String payload = readFile("stub-data/create-subscription-command-for-plea-event.json")
                .replace("%CASE_URN%", CASE_URN)
                .replace("%COURT_ID%", COURT_ID)
                .replace("%ID%", SUBSCRIPTION_ID);

        //when
        makePostCall(getWriteUrl("/subscriptions"),
                " application/vnd.subscriptions.command.create-subscription-by-admin+json",
                payload, USER_ID);


        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionExistMatcher());
        verifySubscription(matchers);
    }

    private static List<Matcher<? super ReadContext>> subscriptionExistMatcher() {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + SUBSCRIPTION_ID + "')].id", contains(SUBSCRIPTION_ID)))
                .build();
    }

    private static void verifySubscription(final List<Matcher<? super ReadContext>> matchers) {

        final String payload = poll(requestParams(getReadUrl("/subscriptions"),
                "application/vnd.subscriptions.query.subscriptions+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(60, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(CoreMatchers.allOf(matchers))).getPayload();
    }
}
