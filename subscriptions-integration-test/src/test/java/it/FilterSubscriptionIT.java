package it;


import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.stubNotificationService;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.verifyEmailNotificationIsRaisedWithValues;
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
import groovy.json.StringEscapeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FilterSubscriptionIT {

    private static final String USER_ID = randomUUID().toString();
    private static String SUBSCRIPTION_ID = randomUUID().toString();
    private static String ORGANISATION_ID = randomUUID().toString();
    private static String COURT_ID;
    private static String CASE_ID;
    private static String CASE_URN;

    @BeforeAll
    public static void init() {
        resetStubs();
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");
        stubNotificationService();
        final ImmutableMap<String, Boolean> features = of("subscriptionsPortal", true);
        FeatureStubber.stubFeaturesFor("subscriptions", features);
    }

    @BeforeEach
    public void setUp() {
        SUBSCRIPTION_ID = randomUUID().toString();
        COURT_ID = randomUUID().toString();
        CASE_URN = STRING.next();
        CASE_ID = randomUUID().toString();
    }

    @Test
    public void shouldRaiseSendEmailRequestedPrivateEvent() {

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
        final String bodyWithCppAppUrl = stringToJsonObjectConverter.convert(eventPayload).getString("body");


        final String expectedPayload = "{\"templateId\":\"d3041cbf-fea3-4c45-9316-eb57d1b2996b\"," +
                "\"sendToAddress\":\"deby@thirdparty.com\"," +
                "\"personalisation\":" +
                format("{\"subject\":\"%s\",", subject) +
                format("\"body\":\"%s\"}}", StringEscapeUtils.escapeJava(bodyWithCppAppUrl));
        verifyEmailNotificationIsRaisedWithValues(asList(expectedPayload));

    }

    @Test
    public void shouldRaiseSendEmailRequestedPrivateEventWithoutDOB() {

        //given
        createSubscription();

        final String payload = readFile("stub-data/public.events.hearing.hearing-resulted.json")
                .replace("COURT_CENTRE_ID", COURT_ID)
                .replace("CASE_ID", CASE_ID)
                .replace("CASE_URN", CASE_URN);

        //when then
        final String body = MessageFormat.format("<h2>Plea entered</h2><p>samba RAMBA{0}a{0}GUILTY</p><a _target=\"blank\" href=\"(.*)prosecution-casefile/case-at-a-glance/{1}\">Access the case </a>for full details.", "<br>", CASE_ID);
        final String subject = format("Case %s - plea entered", CASE_URN);

        final String eventPayload = postMessageToTopicAndVerify(payload,
                "subscriptions.event.send-email-requested",
                "public.events.hearing.hearing-resulted", true,
                withJsonPath("$.subject", equalTo(subject)),
                withJsonPath("$.body", matchesRegex(body)));

        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        final String bodyWithCppAppUrl = stringToJsonObjectConverter.convert(eventPayload).getString("body");


        final String expectedPayload = "{\"templateId\":\"d3041cbf-fea3-4c45-9316-eb57d1b2996b\"," +
                "\"sendToAddress\":\"deby@thirdparty.com\"," +
                "\"personalisation\":" +
                format("{\"subject\":\"%s\",", subject) +
                format("\"body\":\"%s\"}}", StringEscapeUtils.escapeJava(bodyWithCppAppUrl));
        verifyEmailNotificationIsRaisedWithValues(asList(expectedPayload));

    }

    private void createSubscription() {
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

    private List<Matcher<? super ReadContext>> subscriptionExistMatcher() {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + SUBSCRIPTION_ID + "')].id", contains(SUBSCRIPTION_ID)))
                .build();
    }

    private void verifySubscription(final List<Matcher<? super ReadContext>> matchers) {

        final String payload = poll(requestParams(getReadUrl("/subscriptions"),
                "application/vnd.subscriptions.query.subscriptions+json")
                .withHeader(HeaderConstants.USER_ID, USER_ID))
                .timeout(60, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(CoreMatchers.allOf(matchers))).getPayload();
    }
}
