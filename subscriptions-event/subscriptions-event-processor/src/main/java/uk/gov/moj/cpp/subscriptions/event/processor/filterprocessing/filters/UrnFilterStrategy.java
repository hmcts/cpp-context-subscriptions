package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.ArrayList;
import java.util.List;

public class UrnFilterStrategy implements AbstractFilterStrategy {
    private final Filter filter;

    public UrnFilterStrategy(final Subscription subscription) {
        this.filter = subscription.getFilter();
    }

    @Override
    public boolean caseMatches(final ProsecutionCase prosecutionCase) {
        return prosecutionCase
                .getProsecutionCaseIdentifier().getCaseURN().equals(filter.getUrn());
    }

    @Override
    public List<Defendant> filterDefendants(ProsecutionCase prosecutionCase) {
        return new ArrayList<>(prosecutionCase.getDefendants());
    }
}
