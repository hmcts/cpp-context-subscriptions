package uk.gov.moj.cpp.subscriptions.event.processor.events;


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
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.Verdict.verdict;
import static uk.gov.justice.core.courts.VerdictType.verdictType;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.VerdictEnterEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VerdictEnterEventRuleTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String LINE_SEPARATOR = "<br>";

    @Mock
    private ApplicationParameters applicationParameters;

    @Test
    public void defendantsShouldBeRepeatedPerOffence() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffences();

        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);
        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = verdictEnterEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - verdict set"));
        assertThat(emailInfo.getTitle(), is("Verdict set"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("John SMITH. Failing to report an accident. Verdict: Found guilty. John SMITH. Murder. Verdict: Found not guilty. Maggie SMITH - 29 April 1982. Driving while disqualified. Verdict: Found guilty. ")));

    }

    @Test
    public void legalEntityDefendantsShouldBeRepeatedPerOffence() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffencesAndLegalEntityDefendant();

        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);
        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = verdictEnterEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - verdict set"));
        assertThat(emailInfo.getTitle(), is("Verdict set"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("ABC Corporation. Failing to report an accident. Verdict: Found guilty. ABC Corporation. Murder. Verdict: Found not guilty. XYZ Corporation. Driving while disqualified. Verdict: Found guilty. ")));

    }

    @Test
    public void shouldNotExecuteIfThereAreNoDefendantsInProsecutionCase() {
        final ProsecutionCase prosecutionCase = prosecutionCase().withId(CASE_ID).build();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);

        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void shouldNotExecuteIfThereAreNoDefendantsWithAVerdict() {
        final ProsecutionCase prosecutionCase = createCaseWithNoVerdicts();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);

        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void shouldNotExecuteIfThereAreNoDefendantsWithAnOffence() {
        final ProsecutionCase prosecutionCase = createCaseWithNoOffences();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);

        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void shouldExecuteIfThereAreTwoDefendantsOneWithAPleaAndOneWithoutAnOffence() {
        final ProsecutionCase prosecutionCase = createCaseWithMultipleDefendantsHavingAMixOfPleasAndNoOffences();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final VerdictEnterEventRule verdictEnterEventRule = new VerdictEnterEventRule(prosecutionCase, subscription);
        verdictEnterEventRule.setApplicationParameters(applicationParameters);

        assertThat(verdictEnterEventRule.shouldExecute(), equalTo(true));
    }

    private ProsecutionCase createCaseWithMultipleOffences() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
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
                                        createOffenceWithNoVerdict("Failing to report an accident"),
                                        createOffence("Failing to report an accident", "Found guilty"),
                                        createOffence("Murder", "Found not guilty"))
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
                                        createOffenceWithNoVerdict("Failing to report an accident"),
                                        createOffence("Driving while disqualified", "Found guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithMultipleOffencesAndLegalEntityDefendant() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withLegalEntityDefendant(
                                        LegalEntityDefendant.legalEntityDefendant()
                                                .withOrganisation(Organisation.organisation()
                                                        .withName("ABC Corporation")
                                                        .build())
                                                .build()
                                        )
                                .withOffences(asList(
                                        createOffenceWithNoVerdict("Failing to report an accident"),
                                        createOffence("Failing to report an accident", "Found guilty"),
                                        createOffence("Murder", "Found not guilty"))
                                )
                                .build()
                        , defendant()
                                .withLegalEntityDefendant(
                                        LegalEntityDefendant.legalEntityDefendant()
                                                .withOrganisation(Organisation.organisation()
                                                        .withName("XYZ Corporation")
                                                        .build())
                                                .build()
                                )
                                .withOffences(asList(
                                        createOffenceWithNoVerdict("Failing to report an accident"),
                                        createOffence("Driving while disqualified", "Found guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithNoOffences() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
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
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithMultipleDefendantsHavingAMixOfPleasAndNoOffences() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
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
                                        createOffenceWithNoVerdict("Failing to report an accident"),
                                        createOffence("Failing to report an accident", "Found guilty"),
                                        createOffence("Murder", "Found not guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithNoVerdicts() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
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
                                .withOffences(asList(createOffenceWithNoVerdict("Failing to report an accident")))
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
                                .withOffences(asList(createOffenceWithNoVerdict("Failing to report an accident")))
                                .build())
                ).build();
    }

    private Offence createOffence(final String offenceTitle, final String verdict) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .withVerdict(verdict()
                        .withVerdictType(verdictType()
                                .withCategory(verdict)
                                .build())
                        .build())
                .build();
    }

    private Offence createOffenceWithNoVerdict(final String offenceTitle) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .build();
    }
}
