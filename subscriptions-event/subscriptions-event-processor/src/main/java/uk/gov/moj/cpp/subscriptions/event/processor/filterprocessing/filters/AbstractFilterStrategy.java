package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;

public interface AbstractFilterStrategy {

    boolean caseMatches(final ProsecutionCase prosecutionCase);

    List<Defendant> filterDefendants(final ProsecutionCase prosecutionCase);

    static AbstractFilterStrategy createFilter(final Subscription subscription) {
        switch (subscription.getFilter().getFilterType()) {
            case DEFENDANT:
                return new DefendantFilterStrategy(subscription);
            case CASE_REFERENCE:
                return new UrnFilterStrategy(subscription);
            case GENDER:
                return new GenderFilterStrategy(subscription);
            case OFFENCE:
                return new OffenceFilterStrategy(subscription);
            case AGE:
                return new AgeFilterStrategy(subscription);
            default:
                throw new IllegalArgumentException(subscription.getFilter().getFilterType().name());
        }

    }
}
