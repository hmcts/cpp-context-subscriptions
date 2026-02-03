package it;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.subscriptions.helper.EventListener;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.*;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.stubGetUserDetails;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

public class CreateSubscriptionIT {

    private static final String SUBSCRIPTION_CREATED_PUBLIC_EVENT = "public.subscriptions.event.subscription-created";
    private static final String SUBSCRIPTION_ACTIVATE_PUBLIC_EVENT = "public.subscriptions.event.subscription-activated-successfully";
    private static final String SUBSCRIPTION_DEACTIVATE_PUBLIC_EVENT = "public.subscriptions.event.subscription-deactivated-successfully";
    private static final String SUBSCRIPTION_DELETE_PUBLIC_EVENT = "public.subscriptions.event.subscription-deleted-successfully";

    private static String USER_ID;
    private static String ID;
    private static String ORGANISATION_ID;

    @BeforeAll
    public static void init() {
        resetStubs();
        final ImmutableMap<String, Boolean> features = of("subscriptionsPortal", true);
        FeatureStubber.stubFeaturesFor("subscriptions", features);
    }

    @BeforeEach
    public void setUp() {
        ID = randomUUID().toString();
        USER_ID = randomUUID().toString();
        ORGANISATION_ID = randomUUID().toString();
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");
    }

    @Test
    public void shouldCreateSubscriptionByAdmin() {
        createAndVerifySubscription();
    }

    @Test
    public void shouldDeactivateSubscriptionByAdmin() {
        createAndVerifySubscription();
        deactivateAndVerifySubscription();
    }

    @Test
    public void shouldActivateSubscriptionByAdmin() {
        createAndVerifySubscription();
        deactivateAndVerifySubscription();
        activateAndVerifySubscription();
    }

    @Test
    public void shouldDeleteSubscriptionByAdmin() {
        createAndVerifySubscription();
        deleteAndVerifySubscription();
    }

    private void deleteAndVerifySubscription() {
        final EventListener eventListener = new EventListener(SUBSCRIPTION_DELETE_PUBLIC_EVENT);
        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.delete-subscription+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionNotExistMatcher(ID));

        verifySubscription(matchers);
    }

    private void activateAndVerifySubscription() {
        final EventListener eventListener = new EventListener(SUBSCRIPTION_ACTIVATE_PUBLIC_EVENT);
        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.activate-subscription+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionMatcher(ID, true));
        matchers.addAll(eventsMatcher(ID, "CRACKED_OR_INEFFECTIVE_TRAIL",
                "CHANGE_OF_PLEA",
                "REMAND_STATUS",
                "DEFENDANT_APPELLANT_ATTENDANCE",
                "PLEAS_ENTER",
                "VERDICTS_ENTER",
                "PRE_SENTENCE_REPORT_REQUESTED"));
        matchers.addAll(nowsEdtssMatcher(ID, "Custody Warrant on Discharge of Extradition Pending Appeal",
                "Custody Warrant on Extradition"));
        matchers.addAll(subscriberMatcher(ID, true));
        matchers.addAll(filterByCaseURNMatcher(ID));
        matchers.addAll(courtMatcher(ID));
        verifySubscription(matchers);
    }

    private void deactivateAndVerifySubscription() {
        final EventListener eventListener = new EventListener(SUBSCRIPTION_DEACTIVATE_PUBLIC_EVENT);
        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.deactivate-subscription+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionMatcher(ID, false));
        matchers.addAll(eventsMatcher(ID, "CRACKED_OR_INEFFECTIVE_TRAIL",
                "CHANGE_OF_PLEA",
                "REMAND_STATUS",
                "DEFENDANT_APPELLANT_ATTENDANCE",
                "PLEAS_ENTER",
                "VERDICTS_ENTER",
                "PRE_SENTENCE_REPORT_REQUESTED"));
        matchers.addAll(nowsEdtssMatcher(ID, "Custody Warrant on Discharge of Extradition Pending Appeal",
                "Custody Warrant on Extradition"));
        matchers.addAll(subscriberMatcher(ID, false));
        matchers.addAll(filterByCaseURNMatcher(ID));
        matchers.addAll(courtMatcher(ID));
        verifySubscription(matchers);
    }


    private void createAndVerifySubscription() {
        final EventListener eventListener = new EventListener(SUBSCRIPTION_CREATED_PUBLIC_EVENT);
        //given
        final String payload = readFile("stub-data/create-subscription-command.json")
                .replace("%ID%", ID);

        //when
        makePostCall(getWriteUrl("/subscriptions"),
                " application/vnd.subscriptions.command.create-subscription-by-admin+json",
                payload, USER_ID);

        //then
        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscription.id", equalTo(ID)),
                withJsonPath("$.subscription.name", equalTo("Derby Only")),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));


        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionMatcher(ID, true));
        matchers.addAll(eventsMatcher(ID, "CRACKED_OR_INEFFECTIVE_TRAIL",
                "CHANGE_OF_PLEA",
                "REMAND_STATUS",
                "DEFENDANT_APPELLANT_ATTENDANCE",
                "PLEAS_ENTER",
                "VERDICTS_ENTER",
                "PRE_SENTENCE_REPORT_REQUESTED"));
        matchers.addAll(nowsEdtssMatcher(ID, "Custody Warrant on Discharge of Extradition Pending Appeal",
                "Custody Warrant on Extradition"));
        matchers.addAll(subscriberMatcher(ID, true));
        matchers.addAll(filterByCaseURNMatcher(ID));
        matchers.addAll(courtMatcher(ID));

        verifySubscription(matchers);

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

    private List<Matcher<? super ReadContext>> subscriptionNotExistMatcher(final String subscriptionId) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')]", empty()))
                .build();
    }

    private List<Matcher<? super ReadContext>> subscriptionMatcher(final String subscriptionId, final boolean isActive) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].name", contains("Derby Only")))
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].active", contains(isActive)))
                .build();
    }

    private List<Matcher<? super ReadContext>> nowsEdtssMatcher(final String subscriptionId, final String... nowsedt) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].nowsOrEdts[*]", hasItems(nowsedt)))
                .build();
    }

    private List<Matcher<? super ReadContext>> eventsMatcher(final String subscriptionId, final String... events) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].events[*]", hasItems(events)))
                .build();
    }

    private List<Matcher<? super ReadContext>> subscriberMatcher(final String subscriptionId, final boolean isActive) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].subscribers[*].emailAddress", contains("deby@thirdparty.com")))
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].subscribers[*].active", contains(isActive)))
                .build();
    }

    private List<Matcher<? super ReadContext>> filterByCaseURNMatcher(final String subscriptionId) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].filter.filterType", contains("CASE_REFERENCE")))
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].filter.urn", contains("324135134")))
                .build();
    }

    private List<Matcher<? super ReadContext>> courtMatcher(final String subscriptionId) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')]..courts[*].courtId", contains("3803e612-6369-461a-a2cc-25abb374c5cc")))
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')]..courts[*].name", contains("Derby Court House")))
                .build();
    }

}
