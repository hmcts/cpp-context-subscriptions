package uk.gov.moj.cpp.subscriptions.event.processor;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.NowDocumentRequested.nowDocumentRequested;
import static uk.gov.justice.core.courts.nowdocument.NowDocumentRequest.nowDocumentRequest;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscribers.subscribers;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
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

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowDocumentRequestedProcessorTest {

    @Mock
    private Requester requester;

    @Mock
    NotificationEventRuleExecutor notificationEventRuleExecutor;

    @Mock
    SubscriptionsQueryService subscriptionsQueryService;

    @Mock
    HearingService hearingService;

    @InjectMocks
    private NowDocumentRequestedProcessor nowDocumentRequestedProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private EmailInfoSender emailInfoSender;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @BeforeEach
    public void setup() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(true);
    }

    @Test
    public void shouldRaiseCommandWhenProgressionNowDocumentRequestedPublicEventRaised() {

        final UUID courtId = randomUUID();
        final UUID hearingId = randomUUID();

        final NowDocumentRequested nowDocumentRequested = nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest().withHearingId(hearingId).build()).build();
        final Envelope<NowDocumentRequested> nowDocumentRequestedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                nowDocumentRequested);

        final Hearing hearing = hearing().withCourtCentre(courtCentre().withId(courtId).build()).build();

        final List<Subscription> subscriptions = asList(subscription().build());
        when(hearingService.getHearing(any())).thenReturn(Optional.of(hearing));
        when(subscriptionsQueryService.findSubscriptionsByCourt(any(), any())).thenReturn(subscriptions);
        final List<EmailInfo> emailInfos = asList(
                emailInfo()
                        .withSubscription(subscription()
                                .withId(randomUUID())
                                .withName("subscription name")
                                .withSubscribers(asList(subscribers()
                                        .withEmailAddress("abc@xyz.com")
                                        .withActive(true)
                                        .build()))
                                .build())
                        .withSubject("Subject")
                        .withBody("Body")
                        .build());
        when(notificationEventRuleExecutor.execute(nowDocumentRequested, subscriptions)).thenReturn(emailInfos);

        nowDocumentRequestedProcessor.publicProgressionNowDocumentRequested(nowDocumentRequestedEnvelope);

        verify(notificationEventRuleExecutor).execute(nowDocumentRequested, subscriptions);
        verify(subscriptionsQueryService).findSubscriptionsByCourt(courtId, requester);
        verify(emailInfoSender).sendCommand(eq(nowDocumentRequestedEnvelope), eq(emailInfos));
    }

    @Test
    public void shouldNotProcessPublicEventWhenFeatureIsDisabled() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(false);

        final UUID hearingId = randomUUID();

        final NowDocumentRequested nowDocumentRequested = nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest().withHearingId(hearingId).build()).build();
        final Envelope<NowDocumentRequested> nowDocumentRequestedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                nowDocumentRequested);

        nowDocumentRequestedProcessor.publicProgressionNowDocumentRequested(nowDocumentRequestedEnvelope);

        verify(notificationEventRuleExecutor, never()).execute(any(Hearing.class), any());
        verify(subscriptionsQueryService, never()).findSubscriptionsByCourt(any(), any());
        verify(emailInfoSender, never()).sendCommand(any(), any());
    }

    @Test
    void shouldRaiseCommandWhenHearingNowsNowDocumentRequestedPublicEventRaised() {

        final UUID courtId = randomUUID();
        final UUID hearingId = randomUUID();

        final NowDocumentRequested nowDocumentRequested = nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest().withHearingId(hearingId).build()).build();
        final Envelope<NowDocumentRequested> nowDocumentRequestedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                nowDocumentRequested);

        final Hearing hearing = hearing().withCourtCentre(courtCentre().withId(courtId).build()).build();

        final List<Subscription> subscriptions = asList(subscription().build());
        when(hearingService.getHearing(any())).thenReturn(Optional.of(hearing));
        when(subscriptionsQueryService.findSubscriptionsByCourt(any(), any())).thenReturn(subscriptions);
        final List<EmailInfo> emailInfos = asList(
                emailInfo()
                        .withSubscription(subscription()
                                .withId(randomUUID())
                                .withName("subscription name")
                                .withSubscribers(asList(subscribers()
                                        .withEmailAddress("abc@xyz.com")
                                        .withActive(true)
                                        .build()))
                                .build())
                        .withSubject("Subject")
                        .withBody("Body")
                        .build());
        when(notificationEventRuleExecutor.execute(nowDocumentRequested, subscriptions)).thenReturn(emailInfos);

        nowDocumentRequestedProcessor.publicHearingNowsNowDocumentRequested(nowDocumentRequestedEnvelope);

        verify(notificationEventRuleExecutor).execute(nowDocumentRequested, subscriptions);
        verify(subscriptionsQueryService).findSubscriptionsByCourt(courtId, requester);
        verify(emailInfoSender).sendCommand(eq(nowDocumentRequestedEnvelope), eq(emailInfos));
    }

    @Test
    void shouldNotProcessPublicEventHearingNowsWhenFeatureIsDisabled() {
        when(featureControlGuard.isFeatureEnabled("subscriptionsPortal")).thenReturn(false);

        final UUID hearingId = randomUUID();

        final NowDocumentRequested nowDocumentRequested = nowDocumentRequested()
                .withNowDocumentRequest(nowDocumentRequest().withHearingId(hearingId).build()).build();
        final Envelope<NowDocumentRequested> nowDocumentRequestedEnvelope = envelopeFrom(metadataWithRandomUUIDAndName().build(),
                nowDocumentRequested);

        nowDocumentRequestedProcessor.publicHearingNowsNowDocumentRequested(nowDocumentRequestedEnvelope);

        verify(notificationEventRuleExecutor, never()).execute(any(Hearing.class), any());
        verify(subscriptionsQueryService, never()).findSubscriptionsByCourt(any(), any());
        verify(emailInfoSender, never()).sendCommand(any(), any());
    }
}
