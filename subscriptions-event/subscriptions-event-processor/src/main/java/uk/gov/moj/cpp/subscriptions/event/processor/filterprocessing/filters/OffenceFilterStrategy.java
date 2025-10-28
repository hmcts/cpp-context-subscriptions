package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;
import java.util.function.Predicate;

public class OffenceFilterStrategy implements AbstractFilterStrategy {
    private Filter filter;
    private Predicate<String> offencePredicate = a -> nonNull(a) && a.equals(filter.getOffence());

    public OffenceFilterStrategy(final Subscription subscription) {
        this.filter = subscription.getFilter();
    }

    @Override
    public boolean caseMatches(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants()
                .stream()
                .flatMap(a -> a.getOffences().stream())
                .map(Offence::getOffenceCode)
                .anyMatch(offencePredicate);
    }

    @Override
    public List<uk.gov.justice.core.courts.Defendant> filterDefendants(ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants()
                .stream()
                .filter(a -> a.getOffences().stream()
                        .flatMap(b -> a.getOffences().stream())
                        .map(Offence::getOffenceCode)
                        .anyMatch(offencePredicate))
                .collect(toList());
    }
}
