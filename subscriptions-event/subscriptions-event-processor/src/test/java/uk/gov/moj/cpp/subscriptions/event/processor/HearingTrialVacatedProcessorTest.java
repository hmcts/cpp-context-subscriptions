package uk.gov.moj.cpp.subscriptions.event.processor;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.hearing.courts.TrialVacated.trialVacated;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Events.CHANGE_OF_PLEA;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Events.CRACKED_OR_INEFFECTIVE_TRAIL;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.TrialVacated;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.NotificationEventRuleExecutor;
import uk.gov.moj.cpp.subscriptions.event.processor.service.EmailInfoSender;
import uk.gov.moj.cpp.subscriptions.event.processor.service.HearingService;
import uk.gov.moj.cpp.subscriptions.event.processor.service.SubscriptionsQueryService;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingTrialVacatedProcessorTest {

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private Requester requester;

    @Mock
    private NotificationEventRuleExecutor notificationEventRuleExecutor;

    @Mock
    private SubscriptionsQueryService subscriptionsQueryService;

    @InjectMocks
    private HearingTrialVacatedProcessor hearingTrialVacatedProcessor;

    @Mock
    private EmailInfoSender emailInfoSender;

    @Mock
    private HearingService hearingService;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<Subscription>> subscriptionsArgumentCaptor;

    @BeforeEach
    public void setup() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(true);
    }

    @Test
    public void shouldRaiseCommandWhenHearingTrialVacatedPublicEventRaised() {

        final UUID courtId = randomUUID();
        final UUID hearingId = randomUUID();
        final TrialVacated trialVacated =
                trialVacated()
                        .withHearingId(hearingId)
                        .build();

        final Envelope<TrialVacated> trialVacatedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                trialVacated);

        final List<Subscription> subscriptions = asList(subscription()
                .withEvents(asList(CHANGE_OF_PLEA, CRACKED_OR_INEFFECTIVE_TRAIL))
                .build());
        when(subscriptionsQueryService.findSubscriptionsByCourt(courtId, requester)).thenReturn(subscriptions);
        final List<EmailInfo> emailInfos = asList(
                emailInfo()
                        .withSubscription(subscription()
                                .withEvents(asList(CRACKED_OR_INEFFECTIVE_TRAIL))
                                .build())
                        .withSubject("Subject")
                        .withBody("Body")
                        .build());
        final Hearing hearing = hearing().withCourtCentre(courtCentre().withId(courtId).build()).build();
        when(hearingService.getHearing(hearingId)).thenReturn(of(hearing));
        when(notificationEventRuleExecutor.execute(hearing, filterCrackedOrIneffectiveTrial(subscriptions))).thenReturn(emailInfos);

        hearingTrialVacatedProcessor.publicHearingTrialVacated(trialVacatedEnvelope);

        verify(notificationEventRuleExecutor).execute(hearingArgumentCaptor.capture(), subscriptionsArgumentCaptor.capture());
        assertThat(hearingArgumentCaptor.getValue(), equalTo(hearing));
        assertThat(subscriptionsArgumentCaptor.getValue().get(0).getEvents(), hasSize(1));
        assertThat(subscriptionsArgumentCaptor.getValue().get(0).getEvents().get(0), equalTo(CRACKED_OR_INEFFECTIVE_TRAIL));
        verify(subscriptionsQueryService).findSubscriptionsByCourt(courtId, requester);
        verify(emailInfoSender).sendCommand(eq(trialVacatedEnvelope), eq(emailInfos));
    }


    @Test
    public void shouldNotDoAnythingIfHearingIsNotFoundWhenQueryingHearingContext() {

        final UUID hearingId = randomUUID();
        final TrialVacated trialVacated =
                trialVacated()
                        .withHearingId(hearingId)
                        .build();

        final Envelope<TrialVacated> trialVacatedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                trialVacated);

        when(hearingService.getHearing(hearingId)).thenReturn(Optional.empty());

        hearingTrialVacatedProcessor.publicHearingTrialVacated(trialVacatedEnvelope);

        // check no processing calls have been triggered
        verify(notificationEventRuleExecutor, never()).execute(any(Hearing.class), any());
        verify(subscriptionsQueryService, never()).findSubscriptionsByCourt(any(), any());
        verify(emailInfoSender, never()).sendCommand(any(), any());
    }

    @Test
    public void shouldNotProcessPublicEventWhenFeatureIsDisabled() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(false);

        final UUID hearingId = randomUUID();
        final TrialVacated trialVacated =
                trialVacated()
                        .withHearingId(hearingId)
                        .build();

        final Envelope<TrialVacated> trialVacatedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                trialVacated);

        hearingTrialVacatedProcessor.publicHearingTrialVacated(trialVacatedEnvelope);

        verify(notificationEventRuleExecutor, never()).execute(any(Hearing.class), any());
        verify(subscriptionsQueryService, never()).findSubscriptionsByCourt(any(), any());
        verify(emailInfoSender, never()).sendCommand(any(), any());
    }

    private List<Subscription> filterCrackedOrIneffectiveTrial(final List<Subscription> subscriptions) {
        return subscriptions.stream()
                .map(a -> subscription().withValuesFrom(a).withEvents(a.getEvents()
                        .stream().filter(filterEvent -> filterEvent == CRACKED_OR_INEFFECTIVE_TRAIL)
                        .collect(toList())).build())
                .collect(toList());
    }
}
