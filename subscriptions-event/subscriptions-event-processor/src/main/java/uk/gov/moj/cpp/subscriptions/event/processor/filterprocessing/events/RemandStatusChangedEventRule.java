package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.Section.buildSection;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.event.processor.service.HearingService;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class RemandStatusChangedEventRule extends HearingEventRule {

    private Hearing resultedHearing;
    private ProsecutionCase prosecutionCase;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;
    private HearingService hearingService;

    private Predicate<Defendant> bailStatusChanged =
            defendant -> nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getBailStatus())
                    && defendantBailStatusChanged(defendant);

    public RemandStatusChangedEventRule(final Hearing resultedHearing,
                                        final ProsecutionCase prosecutionCase,
                                        final Subscription subscription,
                                        final HearingService hearingService) {
        this.resultedHearing = resultedHearing;
        this.prosecutionCase = prosecutionCase;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
        this.hearingService = hearingService;
    }

    private boolean hasBailStatusChanged() {
        return prosecutionCase.getDefendants()
                .stream()
                .anyMatch(bailStatusChanged);
    }


    @Override
    public boolean shouldExecute() {
        return !isSjpCase() && containsCaseUrn()
                && hasBailStatusChanged() && filterStrategy.caseMatches(prosecutionCase);
    }

    private boolean isSjpCase() {
        return ofNullable(resultedHearing.getIsSJPHearing()).orElse(false);
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
        return "Remand status changed";
    }

    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    private boolean defendantBailStatusChanged(final Defendant resultedDefendant) {

        final Optional<ProsecutionCase> currentProsecutionCaseOptional = findProsecutionCaseFromCurrentHearing();

        return currentProsecutionCaseOptional.map(aCase -> aCase.getDefendants()
                .stream()
                .filter(a -> a.getId().equals(resultedDefendant.getId()))
                .anyMatch(a -> notEqual(findBailStatus(resultedDefendant), findBailStatus(a)))).orElse(false);

    }

    private String findBailStatus(final Defendant defendant) {
        if (isNull(defendant
                .getPersonDefendant().getBailStatus())) {
            return null;
        }
        return defendant
                .getPersonDefendant()
                .getBailStatus()
                .getDescription();

    }

    private Optional<ProsecutionCase> findProsecutionCaseFromCurrentHearing() {
        final Optional<Hearing> hearingOptional = hearingService.getHearing(resultedHearing.getId());

        return hearingOptional.flatMap(hearing -> hearing.getProsecutionCases()
                .stream()
                .filter(a -> a.getId().equals(this.prosecutionCase.getId()))
                .findFirst());
    }

    protected List<Section> getSections() {
        return filterStrategy.filterDefendants(prosecutionCase)
                .stream()
                .filter(bailStatusChanged)
                .map(a -> buildSection(prepareDefendantLine(a), findBailStatus(a)))
                .collect(toList());
    }
}
