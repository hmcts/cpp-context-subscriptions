package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.function.Predicate;

public class DefendantFilterStrategy implements AbstractFilterStrategy {

    private Filter filter;
    private Predicate<Defendant> defendantPredicate = a ->
    {
        final boolean personDefendantPredicate = nonNull(a.getPersonDefendant()) && nonNull(a.getPersonDefendant().getPersonDetails());

        return personDefendantPredicate && filter.getDefendant().getFirstName().equalsIgnoreCase(a.getPersonDefendant().getPersonDetails().getFirstName()) &&
                filter.getDefendant().getLastName().equalsIgnoreCase(a.getPersonDefendant().getPersonDetails().getLastName()) &&
                filter.getDefendant().getDateOfBirth().equals(a.getPersonDefendant().getPersonDetails().getDateOfBirth());
    };


    public DefendantFilterStrategy(final Subscription subscription) {
        this.filter = subscription.getFilter();
    }

    @Override
    public boolean caseMatches(final ProsecutionCase prosecutionCase) {
        return prosecutionCase
                .getDefendants()
                .stream()
                .anyMatch(defendantPredicate);
    }

    @Override
    public List<Defendant> filterDefendants(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants()
                .stream()
                .filter(defendantPredicate)
                .collect(toList());
    }
}
