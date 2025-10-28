package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.Section.buildSection;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class PleaChangedEventRule extends HearingEventRule {

    private ProsecutionCase prosecutionCase;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;
    private static Map<String, String> pleaTitle = new HashMap();

    static {
        pleaTitle.put("CHANGE_TO_GUILTY_MAGISTRATES_COURT", "Change of Plea: Not Guilty to Guilty");
        pleaTitle.put("CHANGE_TO_NOT_GUILTY", "Change of Plea: Guilty to Not Guilty");
    }

    private Predicate<Offence> offencePleaChanged = offence -> nonNull(offence.getPlea())
            && nonNull(offence.getPlea().getPleaValue())
            && asList("CHANGE_TO_GUILTY_MAGISTRATES_COURT", "CHANGE_TO_NOT_GUILTY").contains(offence.getPlea().getPleaValue());

    private Predicate<Defendant> defendantHasPlea = defendant -> nonNull(defendant.getOffences())
            && defendant.getOffences().stream().anyMatch(offencePleaChanged);

    public PleaChangedEventRule(final ProsecutionCase prosecutionCase, final Subscription subscription) {
        this.prosecutionCase = prosecutionCase;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
    }

    private boolean hasPlea() {
        return nonNull(prosecutionCase.getDefendants()) && prosecutionCase.getDefendants()
                .stream()
                .anyMatch(defendantHasPlea);
    }

    private boolean containsCaseUrn() {
        return nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
    }

    @Override
    public boolean shouldExecute() {
        return hasPlea() && containsCaseUrn() && filterStrategy.caseMatches(prosecutionCase);
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
        return "Plea changed";
    }

    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    protected List<Section> getSections() {
        return filterStrategy.filterDefendants(prosecutionCase)
                .stream()
                .filter(defendantHasPlea)
                .flatMap(a -> this.prepareDefendantsInfo(a).stream())
                .collect(toList());
    }


    private List<Section> prepareDefendantsInfo(Defendant defendant) {
        return defendant.getOffences()
                .stream()
                .filter(offencePleaChanged)
                .map(a -> buildSection(prepareDefendantLine(defendant), a.getOffenceTitle(), pleaTitle.get(a.getPlea().getPleaValue())))
                .collect(toList());
    }



}
