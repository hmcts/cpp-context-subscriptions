package it;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static groovy.json.StringEscapeUtils.escapeJava;
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
import static uk.gov.moj.cpp.subscriptions.helper.HearingServiceStub.stubHearingService;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.stubNotificationService;
import static uk.gov.moj.cpp.subscriptions.helper.NotificationServiceStub.verifyEmailNotificationIsRaisedWithValues;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getReadUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getWriteUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.makePostCall;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.stubGetUserDetails;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NowDocumentRequestedProcessorIT {
    private static final String USER_ID = randomUUID().toString();
    private static String SUBSCRIPTION_ID = randomUUID().toString();
    private static String ORGANISATION_ID = randomUUID().toString();
    private static String HEARING_ID = randomUUID().toString();
    private static String COURT_ID = randomUUID().toString();
    private String CASE_URN;

    @BeforeAll
    public static void init() {
        resetStubs();
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");
        stubNotificationService();
        stubHearingService(HEARING_ID, COURT_ID);
        final ImmutableMap<String, Boolean> features = of("subscriptionsPortal", true);
        FeatureStubber.stubFeaturesFor("subscriptions", features);
    }

    @BeforeEach
    public void setUp() {
        SUBSCRIPTION_ID = randomUUID().toString();
        CASE_URN = STRING.next();
    }

    @Test
    void shouldRaiseProgressionSendEmailRequestedPrivateEvent() {
        testSendEmailRequestedEvent(
                "stub-data/public.progression.now-document-requested.json",
                "public.progression.now-document-requested"
        );
    }

    @Test
    void shouldRaiseHearingnowsSendEmailRequestedPrivateEvent() {
        testSendEmailRequestedEvent(
                "stub-data/public.hearingnows.now-document-requested.json",
                "public.hearingnows.now-document-requested"
        );
    }

    private void testSendEmailRequestedEvent(String payloadFile, String eventType) {
        createSubscription();

        String MATERIAL_ID = randomUUID().toString();
        String REQUEST_ID = randomUUID().toString();
        String DEFENDANT_ID = randomUUID().toString();

        final String payload = readFile(payloadFile)
                .replace("%HEARING_ID%", HEARING_ID)
                .replaceAll("%MATERIAL_ID%", MATERIAL_ID)
                .replace("%REQUEST_ID%", REQUEST_ID)
                .replace("%DEFENDANT_ID%", DEFENDANT_ID)
                .replace("%CASE_URN%", CASE_URN);

        final String body = readFile("stub-data/now-edt-email-body.txt");
        final String subject = format("Case %s Lorraine TORRA 16 August 1988 - Custody Warrant on Extradition", CASE_URN);

        postMessageToTopicAndVerify(payload,
                "subscriptions.event.send-email-requested",
                eventType, true,
                withJsonPath("$.subject", equalTo(subject)),
                withJsonPath("$.body", equalTo(body)));

        final String expectedPayload = "{\"templateId\":\"09f8a4c2-96f1-405d-8fb2-047ed456448c\"," +
                "\"sendToAddress\":\"deby@thirdparty.com\"," +
                "\"personalisation\":" +
                format("{\"subject\":\"%s\",", subject) +
                format("\"body\":\"%s\"},", escapeJava(body)) +
                format("\"materialUrl\":\"%s\"}", format("http://localhost:8080/material-query-api/query/api/rest/material/material/%s?stream=true&requestPdf=true", MATERIAL_ID));
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
                "application/vnd.subscriptions.command.create-subscription-by-admin+json",
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
