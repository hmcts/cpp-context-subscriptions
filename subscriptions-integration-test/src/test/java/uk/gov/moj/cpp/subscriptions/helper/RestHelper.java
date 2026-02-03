package uk.gov.moj.cpp.subscriptions.helper;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.StringReader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestHelper.class);
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://" + HOST + ":8080");
    private static final String WRITE_BASE_URL = "/subscriptions-service/command/api/rest/subscriptions";
    private static final String READ_BASE_URL = "/subscriptions-service/query/api/rest/subscriptions";

    public static final int TIMEOUT = 30;

    private static final RestClient restClient = new RestClient();
    private static final int POLL_INTERVAL = 2;

    public static String pollForResponse(final String path, final String mediaType) {
        return pollForResponse(path, mediaType, randomUUID().toString(), status().is(OK));
    }

    public static String pollForResponse(final String path, final String mediaType, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, randomUUID().toString(), payloadMatchers);
    }

    public static String pollForResponse(final String path, final String mediaType, final String userId, final Matcher... payloadMatchers) {
        return pollForResponse(path, mediaType, userId, status().is(OK), payloadMatchers);
    }


    public static String pollForResponse(final String path, final String mediaType, final String userId, final ResponseStatusMatcher responseStatusMatcher, final Matcher... payloadMatchers) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        responseStatusMatcher,
                        payload().isJson(allOf(payloadMatchers))
                )
                .getPayload();
    }

    public static String pollForResponse(final String path,
                                         final String mediaType,
                                         final String userId, List<Matcher<? super ReadContext>> matchers) {
        return poll(requestParams(getReadUrl(path),
                mediaType)
                .withHeader(USER_ID, userId))
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))).getPayload();

    }

    public static JsonObject getJsonObject(final String jsonAsString) {
        final JsonObject payload;
        try (final JsonReader jsonReader = createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    public static void makePostCall(final UUID userId, final String url, final String mediaType, final String payload, final Response.Status status) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        Response response = restClient.postCommand(url, mediaType, payload, map);
        assertThat(response.getStatus(), is(status.getStatusCode()));
    }

    protected static JsonObject makeGetCall(final String userId, final String url, final String mediaType) {
        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId);
        final Response response = restClient.query(url, mediaType, map);
        assertThat(response.getStatus(), is(200));
        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    public static void makePostCall(final String url, final String mediaType, final String payload, final String userId) {
        makePostCall(UUID.fromString(userId), url, mediaType, payload, Response.Status.ACCEPTED);
    }

    public static void makePostCall(final String url, final String mediaType, final String payload, final String userId, final Response.Status status) {
        makePostCall(UUID.fromString(userId), url, mediaType, payload, status);
    }

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }


}
