package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.function.Predicate;


public class AgeFilterStrategy implements AbstractFilterStrategy {
    private Filter filter;
    private Predicate<Defendant> defendantAgePredicate = a ->
            (isAdult(a).equals(filter.getIsAdult()));


    public AgeFilterStrategy(final Subscription subscription) {
        this.filter = subscription.getFilter();
    }

    @Override
    public boolean caseMatches(final ProsecutionCase prosecutionCase) {
        return prosecutionCase
                .getDefendants()
                .stream()
                .anyMatch(defendantAgePredicate);
    }

    @Override
    public List<Defendant> filterDefendants(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants()
                .stream()
                .filter(defendantAgePredicate)
                .collect(toList());
    }

    private Boolean isAdult(final Defendant a) {
        return isNull(a.getIsYouth()) || Boolean.FALSE.equals(a.getIsYouth());
    }
}
