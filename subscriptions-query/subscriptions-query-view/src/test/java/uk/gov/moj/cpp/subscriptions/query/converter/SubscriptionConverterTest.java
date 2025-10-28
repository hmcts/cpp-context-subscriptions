package uk.gov.moj.cpp.subscriptions.query.converter;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.CHANGE_OF_PLEA;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType.CASE_REFERENCE;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType.GENDER;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.Gender.FEMALE;

import uk.gov.moj.cpp.subscriptions.persistence.constants.EventType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.Gender;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Court;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Event;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Filter;
import uk.gov.moj.cpp.subscriptions.persistence.entity.NowsEdt;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class SubscriptionConverterTest {


    @Test
    public void shouldConvertFromSubscriptionEntity() {
        final SubscriptionConverter subscriptionConverter = new SubscriptionConverter(null);

        final Subscription subscription = Subscription.builder()
                .withId(randomUUID())
                .withName("ABCD")
                .withActive(true)
                .withFilters(Filter.builder()
                        .withFilterType(CASE_REFERENCE)
                        .withOffence("offence1")
                        .withUrn("URN123")
                        .withDateOfBirth(LocalDate.now())
                        .withDefendantFirstName("FirstName")
                        .withDefendantLastName("LastName")
                        .withGender(FEMALE)
                        .withAdult(true)
                        .withId(randomUUID())
                        .build())
                .withOrganisationId(randomUUID())
                .withEvents(newHashSet(Event.builder().withId(randomUUID())
                        .withName(CHANGE_OF_PLEA)
                        .withId(randomUUID())
                        .build()))
                .withNowsEdts(newHashSet(NowsEdt.builder()
                        .withId(randomUUID())
                        .withName("A now")
                        .build()))
                .withCourts(newHashSet(Court.builder()
                        .withId(randomUUID())
                        .withCourtId(randomUUID())
                        .withName("Court Name")
                        .build()))
                .build();

        final uk.gov.moj.cpp.subscriptions.json.schemas.Subscription converted = subscriptionConverter.convert(subscription);

        assertThat(converted.getId(), equalTo(subscription.getId()));
        assertThat(converted.getName(), equalTo(subscription.getName()));
        assertThat(converted.getActive(), equalTo(subscription.isActive()));
        assertThat(converted.getFilter().getFilterType().name(), equalTo(subscription.getFilter().getFilterType().name()));
        assertThat(converted.getFilter().getOffence(), equalTo(subscription.getFilter().getOffence()));
        assertThat(converted.getFilter().getIsAdult(), equalTo(subscription.getFilter().isAdult()));
        assertThat(converted.getFilter().getUrn(), equalTo(subscription.getFilter().getUrn()));
        assertThat(converted.getFilter().getDefendant().getDateOfBirth(), equalTo(subscription.getFilter().getDateOfBirth()));
        assertThat(converted.getFilter().getDefendant().getFirstName(), equalTo(subscription.getFilter().getDefendantFirstName()));
        assertThat(converted.getFilter().getDefendant().getLastName(), equalTo(subscription.getFilter().getDefendantLastName()));
        assertThat(converted.getFilter().getGender().name(), equalTo(subscription.getFilter().getGender().name()));
        assertThat(converted.getFilter().getId(), equalTo(subscription.getFilter().getId()));
        assertThat(converted.getEvents().get(0).name(), equalTo(subscription.getEvents().stream().findFirst().orElse(null).getName().name()));
        assertThat(converted.getNowsOrEdts().get(0), equalTo(subscription.getNowsEdts().stream().findFirst().orElse(null).getName()));
        assertThat(converted.getCourts().get(0).getName(), equalTo(subscription.getCourts().stream().findFirst().orElse(null).getName()));
    }

    @Test
    public void shouldConvertFromSubscriptionEntityFilterBySubscriber() {
        final SubscriptionConverter subscriptionConverter = new SubscriptionConverter("test@test.com");

        final Subscription subscription = Subscription.builder()
                .withId(randomUUID())
                .withName("ABCD")
                .withActive(true)
                .withFilters(Filter.builder()
                        .withFilterType(CASE_REFERENCE)
                        .withOffence("offence1")
                        .withUrn("URN123")
                        .withDateOfBirth(LocalDate.now())
                        .withDefendantFirstName("FirstName")
                        .withDefendantLastName("LastName")
                        .withGender(FEMALE)
                        .withAdult(true)
                        .withId(randomUUID())
                        .build())
                .withOrganisationId(randomUUID())
                .withEvents(newHashSet(Event.builder().withId(randomUUID())
                        .withName(CHANGE_OF_PLEA)
                        .withId(randomUUID())
                        .build()))
                .withNowsEdts(newHashSet(NowsEdt.builder()
                        .withId(randomUUID())
                        .withName("A now")
                        .build()))
                .withCourts(newHashSet(Court.builder()
                        .withId(randomUUID())
                        .withCourtId(randomUUID())
                        .withName("Court Name")
                        .build()))
                .withSubscribers(newHashSet(Subscriber.builder()
                        .withId(randomUUID())
                        .withEmailAddress("test@test.com")
                        .withActive(true)
                        .build(),
                        Subscriber.builder()
                                .withId(randomUUID())
                                .withEmailAddress("test1@test.com")
                                .withActive(true)
                                .build()
                        ))
                .build();

        final uk.gov.moj.cpp.subscriptions.json.schemas.Subscription converted = subscriptionConverter.convert(subscription);

        assertThat(converted.getId(), equalTo(subscription.getId()));
        assertThat(converted.getName(), equalTo(subscription.getName()));
        assertThat(converted.getActive(), equalTo(subscription.isActive()));
        assertThat(converted.getFilter().getFilterType().name(), equalTo(subscription.getFilter().getFilterType().name()));
        assertThat(converted.getFilter().getOffence(), equalTo(subscription.getFilter().getOffence()));
        assertThat(converted.getFilter().getIsAdult(), equalTo(subscription.getFilter().isAdult()));
        assertThat(converted.getFilter().getUrn(), equalTo(subscription.getFilter().getUrn()));
        assertThat(converted.getFilter().getDefendant().getDateOfBirth(), equalTo(subscription.getFilter().getDateOfBirth()));
        assertThat(converted.getFilter().getDefendant().getFirstName(), equalTo(subscription.getFilter().getDefendantFirstName()));
        assertThat(converted.getFilter().getDefendant().getLastName(), equalTo(subscription.getFilter().getDefendantLastName()));
        assertThat(converted.getFilter().getGender().name(), equalTo(subscription.getFilter().getGender().name()));
        assertThat(converted.getFilter().getId(), equalTo(subscription.getFilter().getId()));
        assertThat(converted.getEvents().get(0).name(), equalTo(subscription.getEvents().stream().findFirst().orElse(null).getName().name()));
        assertThat(converted.getNowsOrEdts().get(0), equalTo(subscription.getNowsEdts().stream().findFirst().orElse(null).getName()));
        assertThat(converted.getCourts().get(0).getName(), equalTo(subscription.getCourts().stream().findFirst().orElse(null).getName()));
        assertThat(converted.getSubscribers(), hasSize(1));
        assertThat(converted.getSubscribers().get(0).getEmailAddress(), equalTo("test@test.com"));
    }

}
