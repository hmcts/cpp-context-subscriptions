package uk.gov.moj.cpp.subscriptions.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.findAll;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;

public class NotificationServiceStub {

    public static final String NOTIFICATION_NOTIFY_ENDPOINT = "(.*)/rest/notificationnotify/(.*)";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON = "application/vnd.notificationnotify.email+json";


    public static void stubNotificationService() {
        stubPingFor("notificationnotify-service");

        stubFor(post(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT))
                .withHeader(CONTENT_TYPE, equalTo(NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString()))
        );
    }


    public static void verifyEmailNotificationIsRaisedWithValues(final List<String> expectedValues) {
        final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(NOTIFICATION_NOTIFY_ENDPOINT));
        expectedValues.forEach(expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue)));
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> findAll(requestPatternBuilder).size(), CoreMatchers.is(1));
    }
}
