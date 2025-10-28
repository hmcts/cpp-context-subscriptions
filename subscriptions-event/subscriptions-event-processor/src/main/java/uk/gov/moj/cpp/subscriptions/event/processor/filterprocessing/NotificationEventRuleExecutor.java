package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing;


import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.AbstractEventRule.createEvent;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.AbstractEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.NowEdtEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.event.processor.service.HearingService;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationEventRuleExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventRuleExecutor.class);

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private HearingService hearingService;

    public List<EmailInfo> execute(final Hearing hearing, final List<Subscription> subscriptions) {
        if (isNull(hearing) || isNull(hearing.getProsecutionCases())) {
            LOGGER.info("No hearing or prosecution cases => so will not execute the subscriptions rules");
            return emptyList();
        }

        List<EmailInfo> emailInfo = subscriptions
                .stream()
                .filter(Subscription::getActive)
                .flatMap(subscription ->
                        subscription.getEvents()
                                .stream()
                                .flatMap(event ->
                                        hearing.getProsecutionCases()
                                                .stream()
                                                .map(prosecutionCase -> createEvent(hearing, event, prosecutionCase, subscription, applicationParameters, hearingService))
                                                .filter(AbstractEventRule::shouldExecute)
                                                .map(AbstractEventRule::execute)

                                )

                ).collect(toList());
        return emailInfo;
    }

    public List<EmailInfo> execute(final NowDocumentRequested nowDocumentRequested,
                                   final List<Subscription> subscriptions) {

        return subscriptions
                .stream()
                .filter(Subscription::getActive)
                .flatMap(subscription ->
                        subscription.getNowsOrEdts()
                                .stream()
                                .flatMap(nowEdtName ->
                                        nowDocumentRequested
                                                .getNowDocumentRequest()
                                                .getNowContent()
                                                .getCases()
                                                .stream()
                                                .map(prosecutionCase -> new NowEdtEventRule(
                                                        nowDocumentRequested.getNowDocumentRequest().getNowContent(),
                                                        prosecutionCase,
                                                        nowDocumentRequested.getMaterialId(),
                                                        nowEdtName,
                                                        subscription,
                                                        applicationParameters
                                                ))
                                                .filter(AbstractEventRule::shouldExecute)
                                                .map(AbstractEventRule::execute)

                                )

                ).collect(toList());
    }
}
