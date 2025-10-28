package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.event.processor.service.HearingService;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Events;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


import static java.lang.String.format;
import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;

public abstract class AbstractEventRule {

    protected ApplicationParameters applicationParameters;

    public abstract boolean shouldExecute();

    protected abstract Subscription getSubscription();



    public void setApplicationParameters(final ApplicationParameters applicationParameters) {
        this.applicationParameters = applicationParameters;
    }

    public EmailInfo execute() {
        return emailInfo()
                .withTitle(prepareTitle())
                .withSubject(prepareSubject())
                .withBody(prepareBody())
                .withCaseLink(prepareCaseLink())
                .withSubscription(getSubscription())
                .build();
    }

    protected abstract String prepareSubject();

    protected abstract String prepareTitle();

    protected abstract String prepareCaseLink();
    protected abstract String prepareBody();

    public static AbstractEventRule createEvent(final Hearing hearing, final Events eventType,
                                                final ProsecutionCase prosecutionCase,
                                                final Subscription subscription,
                                                final ApplicationParameters applicationParameters,
                                                final HearingService hearingService) {
        AbstractEventRule eventRule;
        switch (eventType) {
            case PLEAS_ENTER:
                eventRule = new PleaEnteredEventRule(prosecutionCase, subscription);
                break;
            case PRE_SENTENCE_REPORT_REQUESTED:
                eventRule = new PreSentenceReportEventRule(prosecutionCase, subscription);
                break;
            case VERDICTS_ENTER:
                eventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
                break;
            case DEFENDANT_APPELLANT_ATTENDANCE:
                eventRule = new DefendantPresentEventRule(hearing, prosecutionCase, subscription);
                break;
            case CHANGE_OF_PLEA:
                eventRule = new PleaChangedEventRule(prosecutionCase, subscription);
                break;
            case CRACKED_OR_INEFFECTIVE_TRAIL:
                eventRule = new TrialEffectivenessEventRule(hearing, prosecutionCase, subscription);
                break;
            case REMAND_STATUS:
                eventRule = new RemandStatusChangedEventRule(hearing, prosecutionCase, subscription, hearingService);
                break;
            default:
                throw new IllegalArgumentException(eventType.toString());
        }
        eventRule.setApplicationParameters(applicationParameters);
        return eventRule;
    }



    static String formatDateOfBirth(final LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
    }

    protected String preparePersonalDefendantInfo(Defendant defendant) {
        if(isNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())){
            return format("%s %s", defendant.getPersonDefendant().getPersonDetails().getFirstName(),
                    defendant.getPersonDefendant().getPersonDetails().getLastName().toUpperCase());
        }else {
            return format("%s %s - %s", defendant.getPersonDefendant().getPersonDetails().getFirstName(),
                    defendant.getPersonDefendant().getPersonDetails().getLastName().toUpperCase(),
                    formatDateOfBirth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()));
        }
    }


}
