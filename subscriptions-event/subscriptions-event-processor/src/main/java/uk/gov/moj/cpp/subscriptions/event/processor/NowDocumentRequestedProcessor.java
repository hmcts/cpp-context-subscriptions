package uk.gov.moj.cpp.subscriptions.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2629", "squid:S1612"})
@ServiceComponent(EVENT_PROCESSOR)
public class NowDocumentRequestedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NowDocumentRequestedProcessor.class);

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    private Requester requester;

    @Inject
    private NotificationEventRuleExecutor notificationEventRuleExecutor;

    @Inject
    private SubscriptionsQueryService subscriptionsQueryService;

    @Inject
    private HearingService hearingService;

    @Inject
    private EmailInfoSender emailInfoSender;

    @Handles("public.progression.now-document-requested")
    public void publicProgressionNowDocumentRequested(final Envelope<NowDocumentRequested> event) {
        processNowDocumentRequested(event, "public.progression.now-document-requested");
    }

    @Handles("public.hearingnows.now-document-requested")
    public void publicHearingNowsNowDocumentRequested(final Envelope<NowDocumentRequested> event) {
        processNowDocumentRequested(event, "public.hearingnows.now-document-requested");
    }

    private void processNowDocumentRequested(final Envelope<NowDocumentRequested> event, final String eventName) {
        if (featureControlGuard.isFeatureEnabled("subscriptionsPortal")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Handling {} {}", eventName, event.payload());
            }

            final UUID hearingId = event.payload().getNowDocumentRequest().getHearingId();
            final Optional<Hearing> hearingOptional = hearingService.getHearing(hearingId);
            if (hearingOptional.isPresent()) {
                final List<Subscription> subscriptionsByCourt = subscriptionsQueryService
                        .findSubscriptionsByCourt(hearingOptional.get().getCourtCentre().getId(), requester);

                final List<EmailInfo> emailInfos = notificationEventRuleExecutor.execute(event.payload(), subscriptionsByCourt);
                emailInfoSender.sendCommand(event, emailInfos);
            } else {
                LOGGER.info("No hearing found in the hearing context with hearing id: {} => so will not execute the subscriptions rules.", hearingId);
            }
        } else {
            LOGGER.info("'subscriptionsPortal' disabled so not processing {} event", eventName);
        }
    }
}
