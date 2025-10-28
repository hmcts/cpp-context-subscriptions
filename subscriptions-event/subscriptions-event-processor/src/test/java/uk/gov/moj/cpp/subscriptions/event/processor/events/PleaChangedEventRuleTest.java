package uk.gov.moj.cpp.subscriptions.event.processor.events;


import static java.lang.System.lineSeparator;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.Plea.plea;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.PleaChangedEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.VerdictEnterEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleaChangedEventRuleTest {

    private static final UUID CASE_ID = randomUUID();

    @Mock
    private ApplicationParameters applicationParameters;

    @Test
    public void defendantsShouldBeRepeatedPerOffence() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffences();

        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final PleaChangedEventRule pleaChangedEventRule = new PleaChangedEventRule(prosecutionCase, subscription);
        pleaChangedEventRule.setApplicationParameters(applicationParameters);
        assertThat(pleaChangedEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = pleaChangedEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - plea changed"));
        assertThat(emailInfo.getTitle(), is("Plea changed"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("John SMITH. Failing to report an accident. Change of Plea: Not Guilty to Guilty. John SMITH. Murder. Change of Plea: Guilty to Not Guilty. Maggie SMITH - 29 April 1982. Driving while disqualified. Change of Plea: Guilty to Not Guilty. ")));

    }

    @Test
    public void legalEntityDefendantsShouldBeRepeatedPerOffence() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffencesAndLegalEntityDefendant();

        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final PleaChangedEventRule pleaChangedEventRule = new PleaChangedEventRule(prosecutionCase, subscription);
        pleaChangedEventRule.setApplicationParameters(applicationParameters);
        assertThat(pleaChangedEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = pleaChangedEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - plea changed"));
        assertThat(emailInfo.getTitle(), is("Plea changed"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("ABC Corporation. Failing to report an accident. Change of Plea: Not Guilty to Guilty. ABC Corporation. Murder. Change of Plea: Guilty to Not Guilty. Maggie SMITH - 29 April 1982. Driving while disqualified. Change of Plea: Guilty to Not Guilty. ")));

    }

    @Test
    public void shouldNotExecuteIfThereAreNoDefendantsInProsecutionCase() {
        final ProsecutionCase prosecutionCase = prosecutionCase().withId(CASE_ID).build();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final PleaChangedEventRule pleaChangedEventRule = new PleaChangedEventRule(prosecutionCase, subscription);
        pleaChangedEventRule.setApplicationParameters(applicationParameters);

        assertThat(pleaChangedEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void shouldNotExecuteIfThereAreNoDefendantsWithAPlea() {
        final ProsecutionCase prosecutionCase = createCaseWithNoPleas();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);

        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(false));
    }

    private ProsecutionCase createCaseWithMultipleOffences() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("URN123")
                        .build())
                .withDefendants(asList(defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("John")
                                                                .withLastName("Smith")
                                                                .build())
                                                .build())
                                .withOffences(asList(
                                        createOffenceWithNonPlea("An offence without plea"),
                                        createOffence("Failing to report an accident", "CHANGE_TO_GUILTY_MAGISTRATES_COURT"),
                                        createOffence("Murder", "CHANGE_TO_NOT_GUILTY")
                                        )
                                )
                                .build()
                        , defendant()
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
                                        createOffenceWithNonPlea("An offence without plea"),
                                        createOffence("Driving while disqualified", "CHANGE_TO_NOT_GUILTY"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithNoPleas() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("URN123")
                        .build())
                .withDefendants(asList(defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("John")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(asList(createOffenceWithNonPlea("An offence without plea"))).build()
                        , defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .build())
                                .withOffences(asList(createOffenceWithNonPlea("An offence without plea"))).build())
                ).build();
    }

    private Offence createOffence(final String offenceTitle, final String plea) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .withPlea(plea().withPleaValue(plea).build())
                .build();
    }

    private Offence createOffenceWithNonPlea(final String offenceTitle) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .build();
    }

    private ProsecutionCase createCaseWithMultipleOffencesAndLegalEntityDefendant() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("URN123")
                        .build())
                .withDefendants(asList(defendant()
                                .withLegalEntityDefendant(
                                        LegalEntityDefendant.legalEntityDefendant()
                                                .withOrganisation(Organisation.organisation()
                                                        .withName("ABC Corporation")
                                                        .build())
                                                .build())
                                .withOffences(asList(
                                        createOffenceWithNonPlea("An offence without plea"),
                                        createOffence("Failing to report an accident", "CHANGE_TO_GUILTY_MAGISTRATES_COURT"),
                                        createOffence("Murder", "CHANGE_TO_NOT_GUILTY")
                                        )
                                )
                                .build()
                        , defendant()
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
                                        createOffenceWithNonPlea("An offence without plea"),
                                        createOffence("Driving while disqualified", "CHANGE_TO_NOT_GUILTY"))
                                )
                                .build())
                ).build();
    }

}
