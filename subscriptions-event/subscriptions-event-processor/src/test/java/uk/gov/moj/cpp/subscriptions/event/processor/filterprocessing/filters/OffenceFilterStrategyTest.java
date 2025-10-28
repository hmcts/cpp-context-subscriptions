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
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.OFFENCE;

import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class OffenceFilterStrategyTest {

    private Subscription subscription = Subscription.subscription()
            .withFilter(filter()
                    .withFilterType(OFFENCE)
                    .withOffence("OffenceCode1")
                    .build())
            .build();
    private OffenceFilterStrategy offenceFilterStrategy = new OffenceFilterStrategy(subscription);

    @Test
    public void caseShouldNotMatchIfNoMatchingDefendant() {
        assertThat(offenceFilterStrategy.caseMatches(createCaseWithNonMatchedDefendants()), equalTo(false));
    }

    @Test
    public void shouldMatchAndFilterDefendantsWhenThereIsAMatchingDefendant() {
        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendants();
        assertThat(offenceFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<uk.gov.justice.core.courts.Defendant> matchedDefendants = offenceFilterStrategy.filterDefendants(prosecutionCase);
        assertThat(matchedDefendants, hasSize(1));
        assertThat(matchedDefendants.get(0), is(prosecutionCase.getDefendants().get(1)));
    }

    @Test
    public void shouldMatchAndFilterDefendantsWhenThereIsAMatchingDefendantWithOutOffenceCode() {
        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendantsWithOneoffenceWithoutOffenceCode();
        assertThat(offenceFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<uk.gov.justice.core.courts.Defendant> matchedDefendants = offenceFilterStrategy.filterDefendants(prosecutionCase);
        assertThat(matchedDefendants, hasSize(1));
        assertThat(matchedDefendants.get(0), is(prosecutionCase.getDefendants().get(1)));
    }


    private ProsecutionCase createCaseWithNonMatchedDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
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

    private ProsecutionCase createCaseWithSomeMatchingDefendantsWithOneoffenceWithoutOffenceCode() {
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
                                createOffence( "Guilty"))
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
                                createOffence("Guilty"))
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

    private Offence createOffence(final String plea) {
        return offence()
                .withPlea(plea().withPleaValue(plea).build())
                .build();
    }


}
