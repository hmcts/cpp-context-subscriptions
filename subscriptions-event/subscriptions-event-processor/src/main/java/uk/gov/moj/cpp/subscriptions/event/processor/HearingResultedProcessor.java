package uk.gov.moj.cpp.subscriptions.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.NotificationEventRuleExecutor;
import uk.gov.moj.cpp.subscriptions.event.processor.service.EmailInfoSender;
import uk.gov.moj.cpp.subscriptions.event.processor.service.SubscriptionsQueryService;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2629", "squid:S1612"})
@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedProcessor.class);

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    private Requester requester;

    @Inject
    private NotificationEventRuleExecutor notificationEventRuleExecutor;

    @Inject
    private SubscriptionsQueryService subscriptionsQueryService;

    @Inject
    private EmailInfoSender emailInfoSender;

    @Handles("public.hearing.resulted")
    public void publicHearingResulted(final Envelope<HearingResulted> event) {
        if (featureControlGuard.isFeatureEnabled("subscriptionsPortal")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Handling public.hearing.resulted {}", event.payload());
            }
            processHearingResulted(event);
        } else {
            LOGGER.info("'subscriptionsPortal' disabled so not processing 'public.hearing.resulted' event");
        }
    }

    @Handles("public.events.hearing.hearing-resulted")
    public void handleHearingResultedPublicEvent(final Envelope<HearingResulted> event) {
        if (featureControlGuard.isFeatureEnabled("subscriptionsPortal")) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Handling public.events.hearing.hearing-resulted {}", event.payload());
            }
            processHearingResulted(event);
        } else {
            LOGGER.info("'subscriptionsPortal' disabled so not processing 'public.events.hearing.hearing-resulted' event");
        }
    }

    private void processHearingResulted(final Envelope<HearingResulted> event) {
        final Hearing hearing = event.payload().getHearing();
        final List<Subscription> subscriptionsByCourt = subscriptionsQueryService
                .findSubscriptionsByCourt(hearing.getCourtCentre().getId(), requester);

        final List<EmailInfo> emailInfos = notificationEventRuleExecutor.execute(hearing, subscriptionsByCourt);
        emailInfoSender.sendCommand(event, emailInfos);
    }
}
