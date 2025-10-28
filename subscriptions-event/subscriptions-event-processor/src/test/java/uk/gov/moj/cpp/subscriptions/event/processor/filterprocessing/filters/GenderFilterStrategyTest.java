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
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.GENDER;

import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.json.schemas.Gender;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GenderFilterStrategyTest {

    private Subscription subscription = Subscription.subscription()
            .withFilter(filter()
                    .withFilterType(GENDER)
                    .withGender(Gender.MALE)
                    .build())
            .build();

    private GenderFilterStrategy genderFilterStrategy = new GenderFilterStrategy(subscription);

    @Test
    public void caseShouldNotMatchIfNoMatchingDefendant() {
        assertThat(genderFilterStrategy.caseMatches(createCaseWithNonMatchedDefendants()), equalTo(false));
    }

    @Test
    public void caseShouldNotMatchIfDefendantIsAnOrganisation() {
        assertThat(genderFilterStrategy.caseMatches(createCaseWithOrganisationDefendants()), equalTo(false));
    }

    private ProsecutionCase createCaseWithOrganisationDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant().withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation().build())
                        .build()))
                .build();
    }

    @Test
    public void shouldMatchAndFilterDefendantsWhenThereIsAMatchingDefendant() {
        final ProsecutionCase prosecutionCase = createCaseWithSomeMatchingDefendants();
        assertThat(genderFilterStrategy.caseMatches(prosecutionCase), equalTo(true));

        final List<uk.gov.justice.core.courts.Defendant> matchedDefendants = genderFilterStrategy.filterDefendants(prosecutionCase);
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
                                                                .withGender(FEMALE)
                                                                .build())
                                                .build())
                                .withOffences(Arrays.asList(
                                        createOffence("Failing to report an accident", "Not guilty"),
                                        createOffence("Murder", "Guilty"))
                                )
                                .build()
                        , defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withGender(FEMALE).build())
                                                .build())
                                .withOffences(Arrays.asList(
                                        createOffence("Driving while disqualified", "Guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithSomeMatchingDefendants() {
        return prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(Arrays.asList(defendant()
                        .withPersonDefendant(
                                personDefendant()
                                        .withPersonDetails(
                                                person()
                                                        .withGender(FEMALE)
                                                        .build())
                                        .build())
                        .withOffences(Arrays.asList(
                                createOffence("Driving while disqualified", "Guilty"))
                        )
                        .build(), defendant()
                        .withPersonDefendant(
                                personDefendant()
                                        .withPersonDetails(
                                                person()
                                                        .withGender(MALE)
                                                        .build())
                                        .build())
                        .withOffences(Arrays.asList(
                                createOffence("Failing to report an accident", "Not guilty"),
                                createOffence("Murder", "Guilty"))
                        )
                        .build())
                ).build();
    }

    private Offence createOffence(final String offenceTitle, final String plea) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .withPlea(plea().withPleaValue(plea).build())
                .build();
    }


}
