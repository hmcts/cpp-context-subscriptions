package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.text.MessageFormat.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.Section.buildSection;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class PreSentenceReportEventRule extends HearingEventRule {
    private static final String PRE_SENTENCE_REPORT_RESULT_DEFINITION_ID = "029d370b-90f5-4650-b985-a61e9ec8db99";
    private ProsecutionCase prosecutionCase;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;
    private Predicate<Defendant> preSentenceReportExists = defendant -> nonNull(defendant.getOffences()) && defendant.getOffences().stream()
            .filter(a -> nonNull(a.getJudicialResults()))
            .flatMap(a -> a.getJudicialResults().stream())
            .anyMatch(a -> nonNull(a.getJudicialResultTypeId()) &&
                    (PRE_SENTENCE_REPORT_RESULT_DEFINITION_ID.equals(a.getJudicialResultTypeId().toString())));

    public PreSentenceReportEventRule(ProsecutionCase prosecutionCase, Subscription subscription) {
        this.prosecutionCase = prosecutionCase;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
    }

    private boolean hasPreSentenceReport() {
        return nonNull(prosecutionCase.getDefendants()) && prosecutionCase.getDefendants()
                .stream()
                .anyMatch(preSentenceReportExists);
    }

    private boolean containsCaseUrn() {
        return nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
    }

    @Override
    public boolean shouldExecute() {
        return hasPreSentenceReport() && containsCaseUrn() && filterStrategy.caseMatches(prosecutionCase);
    }

    @Override
    protected String getCaseUrn() {
        return prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
    }

    @Override
    protected UUID getCaseId() {
        return prosecutionCase.getId();
    }

    @Override
    protected String getTitle() {
        return "Pre-sentence report requested";
    }

    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    protected List<Section> getSections() {
        return filterStrategy.filterDefendants(prosecutionCase)
                .stream()
                .filter(preSentenceReportExists)
                .map(a -> buildSection(prepareDefendantLine(a)))
                .collect(toList());
    }



}
