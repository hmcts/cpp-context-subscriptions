package uk.gov.moj.cpp.subscriptions.event.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.hearing.courts.HearingResulted.hearingResulted;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.NotificationEventRuleExecutor;
import uk.gov.moj.cpp.subscriptions.event.processor.helper.FileResourceObjectMapper;
import uk.gov.moj.cpp.subscriptions.event.processor.service.EmailInfoSender;
import uk.gov.moj.cpp.subscriptions.event.processor.service.SubscriptionsQueryService;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingResultedProcessorTest {

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private Requester requester;

    @Mock
    private NotificationEventRuleExecutor notificationEventRuleExecutor;

    @Mock
    private SubscriptionsQueryService subscriptionsQueryService;

    @InjectMocks
    private HearingResultedProcessor hearingResultedProcessor;

    @Mock
    private EmailInfoSender emailInfoSender;

    private FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();

    @BeforeEach
    public void setup() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(true);
    }

    @Test
    public void shouldRaiseCommandWhenHearingResultedPublicEventReceived() {
        handleHearingResulted(hearingResultedProcessor::publicHearingResulted);
    }

    @Test
    public void shouldRaiseCommandWhenHearingHearingResultedPublicEventReceived() {
        handleHearingResulted(hearingResultedProcessor::handleHearingResultedPublicEvent);
    }

    @Test
    public void shouldNotProcessPublicEventWhenFeatureIsDisabled() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(false);
        handleHearingResultedWhenDisabled(hearingResultedProcessor::handleHearingResultedPublicEvent);
    }

    @Test
    public void shouldRaiseCommandWhenHearingHearingResultedWithDefendantInitiationCodePublicEventReceived() throws IOException {
        handleHearingResultedWithDefendantInitiationCode(hearingResultedProcessor::handleHearingResultedPublicEvent);
    }

    private void handleHearingResulted(final Consumer<Envelope<HearingResulted>> hearingResultedConsumer) {
        final UUID courtId = randomUUID();
        final HearingResulted hearingResulted = hearingResulted().withHearing(hearing().withCourtCentre(courtCentre().withId(courtId).build()).build()).build();
        final Envelope<HearingResulted> hearingResultedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                hearingResulted);

        final List<Subscription> subscriptions = singletonList(subscription().build());
        when(subscriptionsQueryService.findSubscriptionsByCourt(any(), any())).thenReturn(subscriptions);

        final List<EmailInfo> emailInfos = buildEmailInfos();
        when(notificationEventRuleExecutor.execute(hearingResulted.getHearing(), subscriptions)).thenReturn(emailInfos);

        hearingResultedConsumer.accept(hearingResultedEnvelope);

        verify(notificationEventRuleExecutor).execute(hearingResulted.getHearing(), subscriptions);
        verify(subscriptionsQueryService).findSubscriptionsByCourt(courtId, requester);
        verify(emailInfoSender).sendCommand(eq(hearingResultedEnvelope), eq(emailInfos));

    }

    private void handleHearingResultedWithDefendantInitiationCode(final Consumer<Envelope<HearingResulted>> hearingResultedConsumer) throws IOException {
        final HearingResulted hearingResulted = fileResourceObjectMapper.convertFromFile("stub/NotificationEventHearingResultedWithDefendantInitiationCode.json", HearingResulted.class);
        final Envelope<HearingResulted> hearingResultedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                hearingResulted);

        final List<Subscription> subscriptions = singletonList(subscription().build());
        when(subscriptionsQueryService.findSubscriptionsByCourt(any(), any())).thenReturn(subscriptions);

        final List<EmailInfo> emailInfos = buildEmailInfos();
        when(notificationEventRuleExecutor.execute(hearingResulted.getHearing(), subscriptions)).thenReturn(emailInfos);

        hearingResultedConsumer.accept(hearingResultedEnvelope);

        verify(notificationEventRuleExecutor).execute(hearingResulted.getHearing(), subscriptions);
        verify(emailInfoSender).sendCommand(eq(hearingResultedEnvelope), eq(emailInfos));

    }

    private void handleHearingResultedWhenDisabled(final Consumer<Envelope<HearingResulted>> hearingResultedConsumer) {
        final UUID courtId = randomUUID();
        final HearingResulted hearingResulted = hearingResulted().withHearing(hearing().withCourtCentre(courtCentre().withId(courtId).build()).build()).build();
        final Envelope<HearingResulted> hearingResultedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                hearingResulted);

        hearingResultedConsumer.accept(hearingResultedEnvelope);

        verify(notificationEventRuleExecutor, never()).execute(any(Hearing.class), any());
        verify(subscriptionsQueryService, never()).findSubscriptionsByCourt(any(), any());
        verify(emailInfoSender, never()).sendCommand(any(), any());
    }

    private List<EmailInfo> buildEmailInfos() {
        return singletonList(
                emailInfo()
                        .withSubscription(subscription()
                                .withId(randomUUID())
                                .withName("subscription name")
                                .withSubscribers(singletonList(subscribers()
                                        .withEmailAddress("abc@xyz.com")
                                        .withActive(true)
                                        .build()))
                                .build())
                        .withSubject("Subject")
                        .withBody("Body")
                        .build());
    }
}
