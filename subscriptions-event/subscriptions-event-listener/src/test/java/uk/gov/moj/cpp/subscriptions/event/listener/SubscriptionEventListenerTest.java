package uk.gov.moj.cpp.subscriptions.event.listener;

import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Court.court;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Events.CHANGE_OF_PLEA;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.AGE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.DEFENDANT;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.GENDER;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Gender.MALE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated.subscriptionActivated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated.subscriptionCreated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser.subscriptionCreatedByUser;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated.subscriptionDeactivated;
import static uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted.subscriptionDeleted;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionActivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionCreatedByUser;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeactivated;
import uk.gov.moj.cpp.subscriptions.json.schemas.SubscriptionDeleted;
import uk.gov.moj.cpp.subscriptions.persistence.constants.EventType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.Gender;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;
import uk.gov.moj.cpp.subscriptions.persistence.repository.SubscriptionsRepository;

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionEventListenerTest {

    @InjectMocks
    private SubscriptionEventListener subscriptionEventListener;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Metadata metadata;

    @Mock
    private SubscriptionsRepository subscriptionsRepository;

    @Captor
    private ArgumentCaptor<uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription> subscriptionArgumentCaptor;



    @Test
    public void shouldSaveSubscriptionWithFilterAsDefendant() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(randomUUID())
                        .withActive(true)
                        .withNowsOrEdts(asList("Custodial Remand status", "Custodial Defendant Notice"))
                        .withCourts(asList(court().withCourtId(randomUUID()).withName("Court Name").withCourtId(randomUUID()).build()))
                        .withEvents(asList(CHANGE_OF_PLEA))
                        .withFilter(filter().withFilterType(DEFENDANT).withDefendant(defendant()
                                .withDateOfBirth(parse("1961-03-02"))
                                .withFirstName("Defendant First Name")
                                .withLastName("Defendant Last Name")
                                .build()).build())
                        .build())
                .build();
        when(jsonObjectToObjectConverter.convert(payload, SubscriptionCreated.class)).thenReturn(subscriptionCreated);

        subscriptionEventListener.handleSubscriptionCreated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = subscriptionArgumentCaptor.getValue();
        assertThat(subscription.getId(), is(subscriptionCreated.getSubscription().getId()));
        assertThat(subscription.getName(), is(subscriptionCreated.getSubscription().getName()));
        assertThat(subscription.getNowsEdts(), hasSize(2));
        assertThat(subscription.getNowsEdts().stream().anyMatch(s ->s.getName().equals("Custodial Remand status")), is(true));
        assertThat(subscription.getNowsEdts().stream().anyMatch(s ->s.getName().equals("Custodial Defendant Notice")), is(true));
        assertThat(subscription.getEvents(), hasSize(1));
        assertThat(subscription.getEvents().stream().anyMatch(e -> e.getName().equals(EventType.CHANGE_OF_PLEA)), is(true));
        assertThat(subscription.getFilter().getFilterType(), is(FilterType.DEFENDANT));
        assertThat(subscription.getFilter().getDefendantFirstName(), is("Defendant First Name"));
        assertThat(subscription.getFilter().getDefendantLastName(), is("Defendant Last Name"));
        assertThat(subscription.getFilter().getDateOfBirth(), is(parse("1961-03-02")));
        assertThat(subscription.getFilter().isAdult(), nullValue());
        assertThat(subscription.getOrganisationId(), is(subscriptionCreated.getOrganisationId()));
    }


    @Test
    public void shouldSaveSubscriptionWithFilterAsGender() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(randomUUID())
                        .withActive(true)
                        .withNowsOrEdts(asList("Custodial Remand status", "Custodial Defendant Notice"))
                        .withCourts(asList(court().withCourtId(randomUUID()).withName("Court Name").withCourtId(randomUUID()).build()))
                        .withEvents(asList(CHANGE_OF_PLEA))
                        .withFilter(filter().withFilterType(GENDER)
                                .withGender(MALE)
                                .build())
                        .build())
                .build();
        when(jsonObjectToObjectConverter.convert(payload, SubscriptionCreated.class)).thenReturn(subscriptionCreated);

        subscriptionEventListener.handleSubscriptionCreated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = subscriptionArgumentCaptor.getValue();
        assertThat(subscription.getId(), is(subscriptionCreated.getSubscription().getId()));
        assertThat(subscription.getName(), is(subscriptionCreated.getSubscription().getName()));
        assertThat(subscription.getNowsEdts(), hasSize(2));
        assertThat(subscription.getNowsEdts().stream().anyMatch(s ->s.getName().equals("Custodial Remand status")), is(true));
        assertThat(subscription.getNowsEdts().stream().anyMatch(s ->s.getName().equals("Custodial Defendant Notice")), is(true));
        assertThat(subscription.getEvents(), hasSize(1));
        assertThat(subscription.getEvents().stream().anyMatch(e -> e.getName().equals(EventType.CHANGE_OF_PLEA)), is(true));
        assertThat(subscription.getFilter().getFilterType(), is(FilterType.GENDER));
        assertThat(subscription.getFilter().getDefendantFirstName(), nullValue());
        assertThat(subscription.getFilter().getDefendantLastName(),nullValue());
        assertThat(subscription.getFilter().getDateOfBirth(), nullValue());
        assertThat(subscription.getFilter().isAdult(), nullValue());
        assertThat(subscription.getFilter().getGender(), is(Gender.MALE));
        assertThat(subscription.getOrganisationId(), is(subscriptionCreated.getOrganisationId()));
    }


    @Test
    public void shouldSaveSubscriptionWithFilterAsAge() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        final SubscriptionCreated subscriptionCreated = subscriptionCreated()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(randomUUID())
                        .withActive(true)
                        .withFilter(filter().withFilterType(AGE)
                                .withIsAdult(true)
                                .build())
                        .build())
                .build();
        when(jsonObjectToObjectConverter.convert(payload, SubscriptionCreated.class)).thenReturn(subscriptionCreated);

        subscriptionEventListener.handleSubscriptionCreated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = subscriptionArgumentCaptor.getValue();
        assertThat(subscription.getId(), is(subscriptionCreated.getSubscription().getId()));
        assertThat(subscription.getName(), is(subscriptionCreated.getSubscription().getName()));
        assertThat(subscription.getFilter().getFilterType(), is(FilterType.AGE));
        assertThat(subscription.getFilter().getDefendantFirstName(), nullValue());
        assertThat(subscription.getFilter().getDefendantLastName(),nullValue());
        assertThat(subscription.getFilter().getDateOfBirth(), nullValue());
        assertThat(subscription.getFilter().isAdult(), is(true));
        assertThat(subscription.getFilter().getGender(), nullValue());
        assertThat(subscription.getOrganisationId(), is(subscriptionCreated.getOrganisationId()));
    }


    @Test
    public void shouldActivateSubscriptionAndSubscribers() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionActivated subscriptionActivated = subscriptionActivated().withSubscriptionId(randomUUID()).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionActivated.class)).thenReturn(subscriptionActivated);
        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(Subscriber.builder().withId(randomUUID()).withActive(false).build());
        subscribers.add(Subscriber.builder().withId(randomUUID()).withActive(false).build());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionActivated.getSubscriptionId())
                .withActive(false)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriptionActivated.getSubscriptionId())).thenReturn(subscription);
        subscriptionEventListener.handleSubscriptionActivated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription updatedSubscription = subscriptionArgumentCaptor.getValue();
        assertThat(updatedSubscription.getId(), is(subscriptionActivated.getSubscriptionId()));
        assertThat(updatedSubscription.isActive(), is(true));
        assertThat(updatedSubscription.getSubscribers().stream().allMatch(s -> s.isActive() == true), is(true));
    }


    @Test
    public void shouldActivateSubscription() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionActivated subscriptionActivated = subscriptionActivated().withSubscriptionId(randomUUID()).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionActivated.class)).thenReturn(subscriptionActivated);
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionActivated.getSubscriptionId())
                .withActive(false)
                .build();

        when(subscriptionsRepository.findBy(subscriptionActivated.getSubscriptionId())).thenReturn(subscription);
        subscriptionEventListener.handleSubscriptionActivated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription updatedSubscription = subscriptionArgumentCaptor.getValue();
        assertThat(updatedSubscription.getId(), is(subscriptionActivated.getSubscriptionId()));
        assertThat(updatedSubscription.isActive(), is(true));
        assertThat(updatedSubscription.getSubscribers(), empty());
    }


    @Test
    public void shouldDeactivateSubscriptionAndSubscribers() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionDeactivated subscriptionDeactivated = subscriptionDeactivated().withSubscriptionId(randomUUID()).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionDeactivated.class)).thenReturn(subscriptionDeactivated);
        final Set<Subscriber> subscribers = new HashSet<>();
        subscribers.add(Subscriber.builder().withId(randomUUID()).withActive(true).build());
        subscribers.add(Subscriber.builder().withId(randomUUID()).withActive(true).build());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionDeactivated.getSubscriptionId())
                .withActive(true)
                .withSubscribers(subscribers)
                .build();

        when(subscriptionsRepository.findBy(subscriptionDeactivated.getSubscriptionId())).thenReturn(subscription);
        subscriptionEventListener.handleSubscriptionDeactivated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription updatedSubscription = subscriptionArgumentCaptor.getValue();
        assertThat(updatedSubscription.getId(), is(subscriptionDeactivated.getSubscriptionId()));
        assertThat(updatedSubscription.isActive(), is(false));
        assertThat(updatedSubscription.getSubscribers().stream().allMatch(s -> s.isActive() == false), is(true));
    }


    @Test
    public void shouldDeactivateSubscription() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionDeactivated subscriptionDeactivated = subscriptionDeactivated().withSubscriptionId(randomUUID()).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionDeactivated.class)).thenReturn(subscriptionDeactivated);
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionDeactivated.getSubscriptionId())
                .withActive(true)
                .build();

        when(subscriptionsRepository.findBy(subscriptionDeactivated.getSubscriptionId())).thenReturn(subscription);
        subscriptionEventListener.handleSubscriptionDeactivated(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription updatedSubscription = subscriptionArgumentCaptor.getValue();
        assertThat(updatedSubscription.getId(), is(subscriptionDeactivated.getSubscriptionId()));
        assertThat(updatedSubscription.isActive(), is(false));
        assertThat(updatedSubscription.getSubscribers(), empty());
    }

    @Test
    public void shouldDeleteSubscription() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final SubscriptionDeleted subscriptionDeleted = subscriptionDeleted().withSubscriptionId(randomUUID()).build();

        when(jsonObjectToObjectConverter.convert(payload, SubscriptionDeleted.class)).thenReturn(subscriptionDeleted);
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription.builder()
                .withId(subscriptionDeleted.getSubscriptionId())
                .withActive(true)
                .build();

        when(subscriptionsRepository.findBy(subscriptionDeleted.getSubscriptionId())).thenReturn(subscription);
        subscriptionEventListener.handleSubscriptionDeleted(envelope);
        verify(subscriptionsRepository).remove(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription updatedSubscription = subscriptionArgumentCaptor.getValue();
        assertThat(updatedSubscription.getId(), is(subscriptionDeleted.getSubscriptionId()));
    }

    @Test
    public void shouldSaveSubscriptionByUser() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        final SubscriptionCreatedByUser subscriptionCreated = subscriptionCreatedByUser()
                .withOrganisationId(randomUUID())
                .withSubscription(subscription()
                        .withId(randomUUID())
                        .withActive(true)
                        .withNowsOrEdts(asList("Custodial Remand status", "Custodial Defendant Notice"))
                        .withCourts(asList(court().withCourtId(randomUUID()).withName("Court Name").withCourtId(randomUUID()).build()))
                        .withEvents(asList(CHANGE_OF_PLEA))
                        .withFilter(filter().withFilterType(DEFENDANT).withDefendant(defendant()
                                .withDateOfBirth(parse("1961-03-02"))
                                .withFirstName("Defendant First Name")
                                .withLastName("Defendant Last Name")
                                .build()).build())
                        .build())
                .build();
        when(jsonObjectToObjectConverter.convert(payload, SubscriptionCreatedByUser.class)).thenReturn(subscriptionCreated);

        subscriptionEventListener.handleSubscriptionCreatedByUser(envelope);
        verify(subscriptionsRepository).save(subscriptionArgumentCaptor.capture());
        final uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription subscription = subscriptionArgumentCaptor.getValue();
        assertThat(subscription.getId(), is(subscriptionCreated.getSubscription().getId()));
        assertThat(subscription.getName(), is(subscriptionCreated.getSubscription().getName()));
        assertThat(subscription.getNowsEdts(), hasSize(2));
        assertThat(subscription.getNowsEdts().stream().anyMatch(s ->s.getName().equals("Custodial Remand status")), is(true));
        assertThat(subscription.getNowsEdts().stream().anyMatch(s ->s.getName().equals("Custodial Defendant Notice")), is(true));
        assertThat(subscription.getEvents(), hasSize(1));
        assertThat(subscription.getEvents().stream().anyMatch(e -> e.getName().equals(EventType.CHANGE_OF_PLEA)), is(true));
        assertThat(subscription.getFilter().getFilterType(), is(FilterType.DEFENDANT));
        assertThat(subscription.getFilter().getDefendantFirstName(), is("Defendant First Name"));
        assertThat(subscription.getFilter().getDefendantLastName(), is("Defendant Last Name"));
        assertThat(subscription.getFilter().getDateOfBirth(), is(parse("1961-03-02")));
        assertThat(subscription.getFilter().isAdult(), nullValue());
        assertThat(subscription.getOrganisationId(), is(subscriptionCreated.getOrganisationId()));
    }

}