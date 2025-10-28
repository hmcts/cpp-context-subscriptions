package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.function.Predicate;

public class GenderFilterStrategy implements AbstractFilterStrategy {
    private Filter filter;
    private Predicate<Defendant> genderPredicate = a ->
            nonNull(a.getPersonDefendant()) && nonNull(a.getPersonDefendant().getPersonDetails()) &&
                    nonNull(a.getPersonDefendant().getPersonDetails().getGender()) &&
                    filter.getGender().name().equalsIgnoreCase(a.getPersonDefendant().getPersonDetails().getGender().name());

    public GenderFilterStrategy(final Subscription subscription) {
        this.filter = subscription.getFilter();
    }

    @Override
    public boolean caseMatches(final ProsecutionCase prosecutionCase) {
        return prosecutionCase
                .getDefendants()
                .stream()
                .anyMatch(genderPredicate);
    }

    @Override
    public List<uk.gov.justice.core.courts.Defendant> filterDefendants(ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants()
                .stream()
                .filter(genderPredicate)
                .collect(toList());
    }
}
