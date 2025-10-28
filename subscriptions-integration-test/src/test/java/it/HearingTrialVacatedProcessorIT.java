package it;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
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
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;
import static uk.gov.moj.cpp.subscriptions.helper.HearingServiceStub.stubHearingService;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.stubNotificationService;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.verifyEmailNotificationIsRaisedWithValues;
import static uk.gov.moj.cpp.subscriptions.helper.RegexMatcher.matchesRegex;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getReadUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getWriteUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.makePostCall;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.stubGetUserDetails;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.postMessageToTopicNotConsumed;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.ReadContext;
import groovy.json.StringEscapeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HearingTrialVacatedProcessorIT {
    private static final String USER_ID = randomUUID().toString();
    private static final String SUBSCRIPTION_ID = randomUUID().toString();
    private static final String ORGANISATION_ID = randomUUID().toString();
    private static final String COURT_ID = randomUUID().toString();
    private static final String CASE_URN = STRING.next();
    private static final String CASE_ID = randomUUID().toString();

    private String hearingId;

    @BeforeAll
    public static void init() {
        resetStubs();
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");
        stubNotificationService();
        stubFeaturesFor("subscriptions", of("subscriptionsPortal", true));
    }

    @BeforeEach
    public void setUp() {
        hearingId = randomUUID().toString();
    }

    @Test
    public void shouldSendEmailWhenTrialEffectivenessIsSet() {
        stubHearingService(hearingId, COURT_ID, CASE_URN, CASE_ID);

        //given
        createSubscription();

        final String payload = readFile("stub-data/public.hearing.trial-vacated.json")
                .replace("%HEARING_ID%", hearingId);


        //when then
        final String body = readFile("stub-data/email-trial-effectiveness-set.txt")
                .replace("%CASE_URN%", CASE_URN)
                .replace("%CASE_ID%", CASE_ID);

        final String subject = format("Case %s - trial effectiveness set", CASE_URN);

        final String eventPayload = postMessageToTopicAndVerify(payload,
                "subscriptions.event.send-email-requested",
                "public.hearing.trial-vacated", true,
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
    public void shouldNotSendEmailWhenHearingCannotBeFoundWhenQueryingHearingContext() {
        stubHearingService(hearingId);

        //given
        createSubscription();

        final String payload = readFile("stub-data/public.hearing.trial-vacated.json")
                .replace("%HEARING_ID%", hearingId);

        //when then
        postMessageToTopicNotConsumed(payload,
                "subscriptions.event.send-email-requested",
                "public.hearing.trial-vacated");

    }

    private void createSubscription() {
        //given
        final String payload = readFile("stub-data/create-subscription-command-for-cracked-or-ineffective-trial.json")
                .replace("%CASE_URN%", CASE_URN)
                .replace("%COURT_ID%", COURT_ID)
                .replace("%ID%", SUBSCRIPTION_ID);

        //when
        makePostCall(getWriteUrl("/subscriptions"),
                " application/vnd.subscriptions.command.create-subscription-by-admin+json",
                payload, USER_ID);


        verifySubscription(asList(subscriptionExistMatcher()));
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
