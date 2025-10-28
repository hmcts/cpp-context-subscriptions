package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.lang.Boolean.TRUE;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.Section.buildSection;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.UUID;

public class TrialEffectivenessEventRule extends HearingEventRule {

    private Hearing hearing;
    private ProsecutionCase prosecutionCase;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;

    public TrialEffectivenessEventRule(final Hearing hearing, final ProsecutionCase prosecutionCase, final Subscription subscription) {
        this.hearing = hearing;
        this.prosecutionCase = prosecutionCase;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
    }

    @Override
    public boolean shouldExecute() {
        return hasTrialEffectiveness() && containsCaseUrn() && filterStrategy.caseMatches(prosecutionCase);
    }

    private boolean hasTrialEffectiveness() {
        return nonNull(hearing.getCrackedIneffectiveTrial()) ||
                TRUE.equals(hearing.getIsEffectiveTrial());
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
        return "Trial effectiveness set";
    }

    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    protected List<Section> getSections() {
        final String trialEffectiveness = nonNull(hearing.getCrackedIneffectiveTrial()) ? hearing.getCrackedIneffectiveTrial().getType() : "Effective";
        return asList(buildSection(format("Trial for case {0} is {1}", getCaseUrn(), trialEffectiveness)));
    }
}
