package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.Section.buildSection;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class VerdictEnterEventRule extends HearingEventRule {

    private ProsecutionCase prosecutionCase;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;
    private Predicate<Offence> offenceHasVerdict = offence -> nonNull(offence.getVerdict()) && nonNull(offence.getVerdict().getVerdictType());
    private Predicate<Defendant> defendantHasVerdict = defendant -> nonNull(defendant.getOffences()) &&
            defendant.getOffences().stream().anyMatch(offenceHasVerdict);
    private static String VERDICT_TITLE = "Verdict: ";


    public VerdictEnterEventRule(ProsecutionCase prosecutionCase, Subscription subscription) {
        this.prosecutionCase = prosecutionCase;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
    }

    @Override
    public boolean shouldExecute() {
        return hasVerdict() && containsCaseUrn() && filterStrategy.caseMatches(prosecutionCase);
    }

    private boolean hasVerdict() {
        return nonNull(prosecutionCase.getDefendants()) && prosecutionCase.getDefendants()
                .stream()
                .anyMatch(defendantHasVerdict);
    }

    private boolean containsCaseUrn() {
        return nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
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
        return "Verdict set";
    }

    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    protected List<Section> getSections() {
        return filterStrategy.filterDefendants(prosecutionCase)
                .stream()
                .filter(defendantHasVerdict)
                .flatMap(a -> this.prepareDefendantsInfo(a).stream())
                .collect(toList());
    }

    private List<Section> prepareDefendantsInfo(Defendant defendant) {
        return defendant.getOffences()
                .stream()
                .filter(offenceHasVerdict)
                .map(a -> buildSection(prepareDefendantLine(defendant), a.getOffenceTitle(), VERDICT_TITLE + a.getVerdict().getVerdictType().getCategory()))
                .collect(toList());
    }



}
