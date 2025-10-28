package uk.gov.moj.cpp.subscriptions.event.processor.events;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.PleaChangedEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.PreSentenceReportEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import static java.text.MessageFormat.format;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PreSentenceReportEventRuleTest {

    private static final UUID PRE_SENTENCE_REPORT_RESULT_DEFINITION_ID = fromString("029d370b-90f5-4650-b985-a61e9ec8db99");
    private static final UUID NON_PRE_SENTENCE_REPORT_RESULT_DEFINITION_ID = fromString("36c2d1bc-3af4-4cee-9002-da675dfba0bb");

    private static final UUID CASE_ID = randomUUID();

    @Mock
    private ApplicationParameters applicationParameters;

    @Test
    public void shouldNotExecuteWhenNoJudicialResults() {
        final ProsecutionCase prosecutionCase = createCaseWithNoJudicialResults();
        final PreSentenceReportEventRule preSentenceReportEventRule = new PreSentenceReportEventRule(prosecutionCase, buildSubscription());
        assertThat(preSentenceReportEventRule.shouldExecute(), is(false));
    }

    @Test
    public void shouldNotExecuteIfThereAreNoDefendantsInProsecutionCase() {
        final ProsecutionCase prosecutionCase = prosecutionCase().withId(CASE_ID).build();
        final Subscription subscription = subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final PreSentenceReportEventRule preSentenceReportEventRule = new PreSentenceReportEventRule(prosecutionCase, subscription);
        preSentenceReportEventRule.setApplicationParameters(applicationParameters);

        assertThat(preSentenceReportEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void defendantsShouldRepeatOncePerEmail() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffences();

        final Subscription subscription = buildSubscription();
        final PreSentenceReportEventRule preSentenceReportEventRule = new PreSentenceReportEventRule(prosecutionCase, subscription);
        preSentenceReportEventRule.setApplicationParameters(applicationParameters);
        assertThat(preSentenceReportEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = preSentenceReportEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - pre-sentence report requested"));
        assertThat(emailInfo.getBody(),
                is(format("John SMITH. Maggie SMITH - 1 January 1981. "
                        , CASE_ID
                )));
    }

    @Test
    public void legalEntityDefendantsShouldRepeatOncePerEmail() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffencesWithLegalEntityDefendant();

        final Subscription subscription = buildSubscription();
        final PreSentenceReportEventRule preSentenceReportEventRule = new PreSentenceReportEventRule(prosecutionCase, subscription);
        preSentenceReportEventRule.setApplicationParameters(applicationParameters);
        assertThat(preSentenceReportEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = preSentenceReportEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - pre-sentence report requested"));
        assertThat(emailInfo.getBody(),
                is(format("ABC Corporation. XYZ Corporation. "
                        , CASE_ID
                )));
    }

    private Subscription buildSubscription() {
        return Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
    }

    private ProsecutionCase createCaseWithNoJudicialResults() {
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
                                .withOffences(asList(
                                        createOffenceWithNullJudicialResult())
                                )
                                .build()
                        , defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1981, 1, 1))
                                                                .build())
                                                .build())
                                .withOffences(asList(
                                        createOffenceWithNullJudicialResult())
                                )
                                .build())
                ).build();
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
                                        createOffenceWithNonPSR(),
                                        createOffence(),
                                        createOffence())
                                )
                                .build()
                        , defendant()
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1981, 1, 1))
                                                                .build())
                                                .build())
                                .withOffences(asList(
                                        createOffenceWithNonPSR(),
                                        createOffence())
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithMultipleOffencesWithLegalEntityDefendant() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                        .withOrganisation(Organisation.organisation()
                                                .withName("ABC Corporation")
                                                .build())
                                        .build())
                                .withOffences(asList(
                                        createOffenceWithNonPSR(),
                                        createOffence(),
                                        createOffence())
                                )
                                .build()
                        , defendant()
                                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                        .withOrganisation(Organisation.organisation()
                                                .withName("XYZ Corporation")
                                                .build())
                                        .build())
                                .withOffences(asList(
                                        createOffenceWithNonPSR(),
                                        createOffence())
                                )
                                .build())
                ).build();
    }

    private Offence createOffence() {
        return offence()
                .withJudicialResults(asList(judicialResult()
                        .withJudicialResultTypeId(PRE_SENTENCE_REPORT_RESULT_DEFINITION_ID)
                        .build()
                ))
                .build();
    }

    private Offence createOffenceWithNonPSR() {
        return offence()
                .withJudicialResults(asList(judicialResult()
                        .withJudicialResultTypeId(NON_PRE_SENTENCE_REPORT_RESULT_DEFINITION_ID)
                        .build()
                ))
                .build();
    }

    private Offence createOffenceWithNullJudicialResult() {
        return offence()
                .withJudicialResults(null)
                .build();
    }
}
