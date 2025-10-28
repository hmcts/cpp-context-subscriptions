package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;

import static java.util.Arrays.asList;
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
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.DEFENDANT;

import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Defendant;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DefendantFilterStrategyTest {

    private Subscription subscription = Subscription.subscription()
            .withFilter(filter()
                    .withFilterType(DEFENDANT)
                    .withDefendant(Defendant.defendant()
                            .withFirstName("JOHN")
                            .withLastName("SMITH")
                            .withDateOfBirth(LocalDate.of(1982, 4, 29))
                            .build())
                    .build())
            .build();
    private DefendantFilterStrategy defendantFilterStrategy = new DefendantFilterStrategy(subscription);

    @Test
    public void caseShouldNotMatchIfNoMatchingDefendant() {
        assertThat(defendantFilterStrategy.caseMatches(createCaseWithNonMatchedDefendants()), equalTo(false));
    }

    @Test
    public void caseShouldNotMatchIfDefendantIsAnOrganisation() {
        assertThat(defendantFilterStrategy.caseMatches(createCaseWithOrganisationDefendants()), equalTo(false));
    }

    @Test
    public void caseShouldNotMatchIfDefendantInformationIsMissingFromCase() {
        assertThat(defendantFilterStrategy.caseMatches(createCaseWithDefendantInfoMissing()), equalTo(false));
    }

    @Test
    public void shouldMatchAndFilterDefendantsWhenThereIsAMatchingDefendant() {
        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendants();
        assertThat(defendantFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<uk.gov.justice.core.courts.Defendant> matchedDefendants = defendantFilterStrategy.filterDefendants(prosecutionCase);
        assertThat(matchedDefendants, hasSize(1));
        assertThat(matchedDefendants.get(0), is(prosecutionCase.getDefendants().get(1)));
    }


    private ProsecutionCase createCaseWithNonMatchedDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("John1")
                                                                .withLastName("Smith1")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(asList(
                                        createOffence("Failing to report an accident", "Not guilty"),
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
                                .withOffences(asList(
                                        createOffence("Driving while disqualified", "Guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithSomeMatchingDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                        .withPersonDefendant(
                                personDefendant()
                                        .withPersonDetails(
                                                person()
                                                        .withFirstName("Maggie")
                                                        .withLastName("Smith")
                                                        .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                        .build())
                                        .build())
                        .withOffences(asList(
                                createOffence("Driving while disqualified", "Guilty"))
                        )
                        .build(), defendant()
                        .withPersonDefendant(
                                personDefendant()
                                        .withPersonDetails(
                                                person()
                                                        .withFirstName("John")
                                                        .withLastName("Smith")
                                                        .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                        .build())
                                        .build())
                        .withOffences(asList(
                                createOffence("Failing to report an accident", "Not guilty"),
                                createOffence("Murder", "Guilty"))
                        )
                        .build())
                ).build();
    }

    private ProsecutionCase createCaseWithDefendantInfoMissing() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                        .withPersonDefendant(
                                personDefendant()
                                        .withPersonDetails(
                                                person().build())
                                        .build())
                        .withOffences(asList(
                                createOffence("Driving while disqualified", "Guilty"))
                        )
                        .build(), defendant()
                        .withPersonDefendant(
                                personDefendant()
                                        .withPersonDetails(
                                                person().build())
                                        .build())
                        .withOffences(asList(
                                createOffence("Failing to report an accident", "Not guilty"),
                                createOffence("Murder", "Guilty"))
                        )
                        .build())
                ).build();
    }

    private ProsecutionCase createCaseWithOrganisationDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant().withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation().build())
                        .build()))
                .build();
    }

    private Offence createOffence(final String offenceTitle, final String plea) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .withPlea(plea().withPleaValue(plea).build())
                .build();
    }

}
