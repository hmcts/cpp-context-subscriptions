package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.Plea.plea;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class UrnFilterStrategyTest {

    private Subscription subscription = Subscription.subscription()
            .withFilter(filter()
                    .withFilterType(CASE_REFERENCE)
                    .withUrn("URN123")
                    .build())
            .build();
    private UrnFilterStrategy urnFilterStrategy = new UrnFilterStrategy(subscription);

    @Test
    public void caseShouldNotMatchIfNoMatchingDefendant() {
        assertThat(urnFilterStrategy.caseMatches(createCaseWithNonMatchedDefendants()), equalTo(false));
    }

    @Test
    public void shouldMatchAndFilterAllDefendants() {
        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendants();
        assertThat(urnFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<uk.gov.justice.core.courts.Defendant> matchedDefendants = urnFilterStrategy.filterDefendants(prosecutionCase);
        assertThat(matchedDefendants, hasSize(2));
        assertThat(matchedDefendants.get(0), is(prosecutionCase.getDefendants().get(0)));
        assertThat(matchedDefendants.get(1), is(prosecutionCase.getDefendants().get(1)));
    }


    private ProsecutionCase createCaseWithNonMatchedDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN9999").build())
                .withDefendants(Arrays.asList(defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("John1")
                                                                .withLastName("Smith1")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(Arrays.asList(
                                        createOffence("OffenceCode999", "Not guilty"),
                                        createOffence("Murder", "Guilty"))
                                )
                                .build()
                        , defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie1")
                                                                .withLastName("Smith1")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(Arrays.asList(
                                        createOffence("OffenceCode999", "Guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithSomeMatchingDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(Arrays.asList( defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(Arrays.asList(
                                        createOffence("OffenceCode3", "Guilty"))
                                )
                                .build(),defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("John")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(Arrays.asList(
                                        createOffence("OffenceCode1", "Not guilty"),
                                        createOffence("OffenceCode2", "Guilty"))
                                )
                                .build())
                ).build();
    }

    private Offence createOffence(final String offenceCode, final String plea) {
        return offence()
                .withOffenceCode(offenceCode)
                .withPlea(plea().withPleaValue(plea).build())
                .build();
    }


}
