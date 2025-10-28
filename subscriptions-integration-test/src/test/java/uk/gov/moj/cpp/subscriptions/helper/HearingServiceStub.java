package uk.gov.moj.cpp.subscriptions.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;


public class HearingServiceStub {
    private static final String HEARING_ENDPOINT = "/hearing-service/query/api/rest/hearing/hearings/{0}";
    private static final String HEARING_CONTENT_TYPE = "application/vnd.hearing.get.hearing+json";
    private static final String EMPTY_RESPONSE = "{}";
    private static final int HTTP_STATUS_OK = 200;

    public static void stubHearingService(final String hearingId,
                                          final String courtId) {
        stubHearingService(hearingId, courtId, STRING.next(), randomUUID().toString());
    }

    public static void stubHearingService(final String hearingId,
                                          final String courtId,
                                          final String caseUrn,
                                          final String caseId) {

        final String payload = readFile("stub-data/hearing.get.hearing.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_ID", courtId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("CASE_URN", caseUrn);

        stubFor(get(urlPathEqualTo(format(HEARING_ENDPOINT, hearingId)))
                .willReturn(aResponse()
                        .withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, HEARING_CONTENT_TYPE)
                        .withBody(payload)
                )
        );
    }

    public static void stubHearingService(final String hearingId) {
        stubFor(get(urlPathEqualTo(format(HEARING_ENDPOINT, hearingId)))
                .willReturn(aResponse()
                        .withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, HEARING_CONTENT_TYPE)
                        .withBody(EMPTY_RESPONSE)
                )
        );

    }
}
