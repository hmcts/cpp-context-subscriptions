package uk.gov.moj.cpp.subscriptions.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

import java.util.UUID;

public class StubUtil {

    private static final String USER_DETAILS_MEDIA_TYPE = "application/vnd.usersgroups.logged-in-user-details+json";
    private static final String USER_DETAILS_URL = "/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user.*";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int HTTP_STATUS_OK = 200;

    static {
        resetStubs();
        configureFor(HOST, 8080);
    }

    public static void resetStubs() {
        stubPingFor("usersgroups-service");
    }

    public static void stubGetUserDetails(final String userId, final String organisationId, final String fileName) {
        stubPingFor("usersgroups-service");

        final String payload = readFile(fileName)
                .replace("USER_ID", userId)
                .replace("ORGANISATION_ID", organisationId);

        stubPingFor("usersgroups-service");

        stubFor(get(urlPathMatching(USER_DETAILS_URL))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, USER_DETAILS_MEDIA_TYPE)
                        .withBody(payload)));

    }

    public static void stubGetUserDetails(final String userId, final String organisationId, final String emailAddress, final String fileName) {
        stubPingFor("usersgroups-service");

        final String payload = readFile(fileName)
                .replace("USER_ID", userId)
                .replace("EMAIL", emailAddress)
                .replace("ORGANISATION_ID", organisationId);

        stubPingFor("usersgroups-service");

        stubFor(get(urlPathMatching(USER_DETAILS_URL))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, USER_DETAILS_MEDIA_TYPE)
                        .withBody(payload)));

    }

}


