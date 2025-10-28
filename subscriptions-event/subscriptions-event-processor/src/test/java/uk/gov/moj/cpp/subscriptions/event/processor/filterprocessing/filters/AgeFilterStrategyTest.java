package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.AGE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.List;

import org.junit.jupiter.api.Test;

public class AgeFilterStrategyTest {


    @Test
    public void caseShouldNotMatchIfNoMatchingDefendant() {
        final AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy(createFilter(false));
        assertThat(ageFilterStrategy.caseMatches(createCaseWithNonMatchedDefendants()), equalTo(false));
    }

    @Test
    public void shouldMatchAndFilterDefendantsWhenThereIsAMatchingAdultDefendant() {
        final AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy(createFilter(true));

        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendants();
        assertThat(ageFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<Defendant> matchedDefendants = ageFilterStrategy.filterDefendants(prosecutionCase);
        assertThat(matchedDefendants, hasSize(2));
        assertThat(matchedDefendants.get(0), is(prosecutionCase.getDefendants().get(1)));
        assertThat(matchedDefendants.get(1), is(prosecutionCase.getDefendants().get(2)));
    }

    @Test
    public void shouldMatchAndFilterDefendantsWhenThereIsAMatchingYouthDefendant() {
        final AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy(createFilter(false));

        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendants();
        assertThat(ageFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<Defendant> matchedDefendants = ageFilterStrategy.filterDefendants(prosecutionCase);
        assertThat(matchedDefendants, hasSize(1));
        assertThat(matchedDefendants.get(0), is(prosecutionCase.getDefendants().get(0)));
    }


    private ProsecutionCase createCaseWithNonMatchedDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withIsYouth(null)
                                .build()
                        , defendant()
                                .withIsYouth(null)
                                .build())
                ).build();
    }

    private Subscription createFilter(boolean isAdult) {
        return subscription()
                .withFilter(filter()
                        .withFilterType(AGE)
                        .withIsAdult(isAdult)
                        .build())
                .build();
    }

    private ProsecutionCase createCaseWithSomeMatchingDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withIsYouth(true)
                                .build()
                        , defendant()
                                .withIsYouth(false)
                                .build()
                        , defendant()
                                .withIsYouth(null)
                                .build())
                ).build();
    }

}
