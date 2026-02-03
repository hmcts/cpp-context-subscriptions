package it;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getReadUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.getWriteUrl;
import static uk.gov.moj.cpp.subscriptions.helper.RestHelper.makePostCall;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.subscriptions.helper.StubUtil.stubGetUserDetails;
import static uk.gov.moj.cpp.subscriptions.helper.TestUtil.readFile;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.subscriptions.helper.EventListener;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SubscriberIT extends BaseIT {

    private static final String SUBSCRIPTION_CREATED_PUBLIC_EVENT = "public.subscriptions.event.subscription-created";
    private static final String SUBSCRIPTION_UNSUBSCRIBE_PUBLIC_EVENT = "public.subscriptions.event.subscription-unsubscribed-successfully";
    private static final String SUBSCRIPTION_SUBSCRIBE_PUBLIC_EVENT = "public.subscriptions.event.subscription-subscribed-successfully";
    private static final String SUBSCRIPTION_DELETE_PUBLIC_EVENT = "public.subscriptions.event.subscription-deleted-successfully";
    private static final String SUBSCRIBER_DELETE_FAILED_PUBLIC_EVENT = "public.subscriptions.event.subscriber-delete-failed";
    private static final String SUBSCRIBER_DELETE_PUBLIC_EVENT = "public.subscriptions.event.subscriber-deleted";
    private static final String SUBSCRIPTION_DEACTIVATE_PUBLIC_EVENT = "public.subscriptions.event.subscription-deactivated-successfully";

    public static final String EMAIL_ADDRESS = "richard.chapman@acme.com";


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
    }

    @Test
    public void shouldSubscriberUnSubscribeForGivenSubscription() {
        createAndVerifySubscription();
        unsubscribeAndVerifySubscription(false);
    }

    @Test
    public void shouldSubscriberSubscribeToUnSubscribeSubscription() {
        createAndVerifySubscription();
        unsubscribeAndVerifySubscription(false);
        subscribeAndVerifySubscription();
    }

    @Test
    public void shouldNotDeleteSubscriberWhenGivenSubscriptionAlreadyDeleted() {
        createAndVerifySubscription();
        deleteAndVerifySubscription();
        deleteSubscriberFailed("Subscription does not exist");
    }

    @Test
    public void shouldNotDeleteSubscriberWhenGivenSubscriberNotSubscribedToSubscription() {
        createAndVerifySubscription("stub-data/create-subscription-command.json", "deby@thirdparty.com");
        deleteSubscriberFailed("Subscriber does not subscribe to given subscription");
    }

    @Test
    public void shouldDeleteSubscriptionWhenOnlySubscriberDeletingSubscription() {
        createAndVerifySubscription();
        deleteSubscriberAndSubscriptionAndVerify();
    }

    @Test
    public void shouldDeleteSubscriberOnlyWhenSubscriptionHaveOtherActiveSubscriptions() {
        createAndVerifySubscription("stub-data/create-subscription-command-with-two-subscribers.json", "deby@thirdparty.com");
        deleteSubscriberVerify();
    }

    @Test
    public void shouldDeleteSubscriberAndDeactivateSubscriptionWhenGivenSubscriberIsOnlySubscriberActive() {
        createAndVerifySubscription("stub-data/create-subscription-command-with-two-subscribers.json", "deby@thirdparty.com");
        unsubscribeAndVerifySubscription(true);
        deleteSubscriberAndDeactivateSubscriptionAndVerify();
    }


    private void deleteSubscriberVerify() {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-standard-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIBER_DELETE_PUBLIC_EVENT);


        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.delete-subscriber+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionMatcher(ID, true));
        matchers.addAll(subscriberNotExistMatcher(ID, EMAIL_ADDRESS));
        matchers.addAll(subscriberMatcher(ID, true, "deby@thirdparty.com"));
        verifySubscription(matchers);
    }

    private void createAndVerifySubscription(final String fileName, final String emailAddress) {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIPTION_CREATED_PUBLIC_EVENT);
        //given
        final String payload = readFile(fileName)
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
        matchers.addAll(subscriberMatcher(ID, true, emailAddress));
        verifySubscription(matchers);
    }

    private void deleteSubscriberAndDeactivateSubscriptionAndVerify() {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "deby@thirdparty.com", "mock-data/get-user-details-for-standard-subscription-email.json");

        final EventListener eventListener = new EventListener(SUBSCRIBER_DELETE_PUBLIC_EVENT);

        final EventListener deleteSubscriptionEventListener = new EventListener(SUBSCRIPTION_DEACTIVATE_PUBLIC_EVENT);

        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.delete-subscriber+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        assertThat(deleteSubscriptionEventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionMatcher(ID, false));
        matchers.addAll(subscriberNotExistMatcher(ID, "deby@thirdparty.com"));
        verifySubscription(matchers);
    }

    private void deleteSubscriberAndSubscriptionAndVerify() {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-standard-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIBER_DELETE_PUBLIC_EVENT);

        final EventListener deleteSubscriptionEventListener = new EventListener(SUBSCRIPTION_DELETE_PUBLIC_EVENT);

        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.delete-subscriber+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        assertThat(deleteSubscriptionEventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionNotExistMatcher(ID));

        verifySubscription(matchers);
    }

    private void deleteSubscriberFailed(final String reason) {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-standard-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIBER_DELETE_FAILED_PUBLIC_EVENT);
        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.delete-subscriber+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID)),
                withJsonPath("$.reason", equalTo(reason))
        )));
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


    private void subscribeAndVerifySubscription() {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-standard-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIPTION_SUBSCRIBE_PUBLIC_EVENT);
        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.subscribe+json",
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
        matchers.addAll(subscriberMatcher(ID, true, EMAIL_ADDRESS));
        matchers.addAll(filterByCaseURNMatcher(ID));
        matchers.addAll(courtMatcher(ID));

        verifySubscription(matchers);
    }

    private void unsubscribeAndVerifySubscription(final boolean subscriptionActive) {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-standard-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIPTION_UNSUBSCRIBE_PUBLIC_EVENT);
        //given
        final String payload = createObjectBuilder().build().toString();

        //when
        makePostCall(getWriteUrl(format("/subscriptions/%s", ID)),
                "application/vnd.subscriptions.command.unsubscribe+json",
                payload, USER_ID);

        assertThat(eventListener.retrieveMessage(), isJson(allOf(
                withJsonPath("$.subscriptionId", equalTo(ID)),
                withJsonPath("$.organisationId", equalTo(ORGANISATION_ID))
        )));

        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.addAll(subscriptionMatcher(ID, subscriptionActive));
        matchers.addAll(eventsMatcher(ID, "CRACKED_OR_INEFFECTIVE_TRAIL",
                "CHANGE_OF_PLEA",
                "REMAND_STATUS",
                "DEFENDANT_APPELLANT_ATTENDANCE",
                "PLEAS_ENTER",
                "VERDICTS_ENTER",
                "PRE_SENTENCE_REPORT_REQUESTED"));
        matchers.addAll(nowsEdtssMatcher(ID, "Custody Warrant on Discharge of Extradition Pending Appeal",
                "Custody Warrant on Extradition"));
        matchers.addAll(subscriberMatcher(ID, false, EMAIL_ADDRESS));
        matchers.addAll(filterByCaseURNMatcher(ID));
        matchers.addAll(courtMatcher(ID));

        verifySubscription(matchers);
    }


    private void createAndVerifySubscription() {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");

        final EventListener eventListener = new EventListener(SUBSCRIPTION_CREATED_PUBLIC_EVENT);
        //given
        final String payload = readFile("stub-data/create-subscription-for-specific-subscriber-command.json")
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
        matchers.addAll(subscriberMatcher(ID, true, EMAIL_ADDRESS));
        matchers.addAll(filterByCaseURNMatcher(ID));
        matchers.addAll(courtMatcher(ID));
        verifySubscription(matchers);
    }

    private void verifySubscription(final List<Matcher<? super ReadContext>> matchers) {
        stubGetUserDetails(USER_ID, ORGANISATION_ID, "mock-data/user-and-groups-get-user-details-for-manage-subscription.json");

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

    private List<Matcher<? super ReadContext>> subscriberNotExistMatcher(final String subscriptionId, final String emailAddress) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].subscribers[?(@.emailAddress == '" + emailAddress + "')]", empty()))
                .build();
    }

    private List<Matcher<? super ReadContext>> subscriberMatcher(final String subscriptionId, final boolean isActive, final String emailAddress) {
        return ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].subscribers[?(@.emailAddress == '" + emailAddress + "')]"))
                .add(withJsonPath("$..subscriptions[?(@.id == '" + subscriptionId + "')].subscribers[?(@.emailAddress == '" + emailAddress + "')].active", contains(isActive)))
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
