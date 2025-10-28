package uk.gov.moj.cpp.subscriptions.event.processor;


import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Events.CRACKED_OR_INEFFECTIVE_TRAIL;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.TrialVacated;
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


@ServiceComponent(EVENT_PROCESSOR)
public class HearingTrialVacatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingTrialVacatedProcessor.class);

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


    @Handles("public.hearing.trial-vacated")
    public void publicHearingTrialVacated(final Envelope<TrialVacated> event) {
        if (featureControlGuard.isFeatureEnabled("subscriptionsPortal")) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Handling public.hearing.trial-vacated {}", event.payload());
            }

            final UUID hearingId = event.payload().getHearingId();
            final Optional<Hearing> hearingOptional = hearingService.getHearing(hearingId);

            if (hearingOptional.isPresent()) {
                final Hearing hearing = hearingOptional.get();
                final List<Subscription> subscriptionsByCourt = subscriptionsQueryService
                        .findSubscriptionsByCourt(hearing.getCourtCentre().getId(), requester)
                        .stream()
                        .map(a -> subscription()
                                .withValuesFrom(a)
                                .withEvents(a.getEvents()
                                        .stream()
                                        .filter(filterEvent -> filterEvent == CRACKED_OR_INEFFECTIVE_TRAIL)
                                        .collect(toList())).build())
                        .collect(toList());

                final List<EmailInfo> emailInfos = notificationEventRuleExecutor.execute(hearing, subscriptionsByCourt);
                emailInfoSender.sendCommand(event, emailInfos);
            } else {
                LOGGER.info("No hearing found in the hearing context with hearing id: {} => so will not execute the subscriptions rules.", hearingId);
            }
        } else {
            LOGGER.info("'subscriptionsPortal' disabled so not processing 'public.hearing.trial-vacated' event");
        }
    }
}
