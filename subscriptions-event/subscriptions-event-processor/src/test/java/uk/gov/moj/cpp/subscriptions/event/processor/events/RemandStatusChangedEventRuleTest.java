package uk.gov.moj.cpp.subscriptions.event.processor.events;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.RemandStatusChangedEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.event.processor.service.HearingService;
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
public class RemandStatusChangedEventRuleTest {

    private static final UUID CASE_ID = randomUUID();
    private UUID HEARING_ID = randomUUID();
    private UUID DEFENDANT_1 = randomUUID();
    private UUID DEFENDANT_2 = randomUUID();
    private UUID DEFENDANT_3 = randomUUID();
    private UUID DEFENDANT_4 = randomUUID();
    private static final String LINE_SEPARATOR = "<br>";


    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private HearingService hearingService;

    @Test
    public void shouldNotExecuteWhenNoBailStatus() {
        final RemandStatusChangedEventRule remandStatusChangedEventRule =
                new RemandStatusChangedEventRule(buildResultedHearing(),
                        buildCaseWithNoBailStatus(),
                        buildSubscription(),
                        null);
        assertThat(remandStatusChangedEventRule.shouldExecute(), is(false));
    }

    @Test
    public void shouldNotExecuteWhenSjpCase() {
        final RemandStatusChangedEventRule remandStatusChangedEventRule =
                new RemandStatusChangedEventRule(buildSjpHearing(),
                        null,
                        buildSubscription(),
                        null);
        assertThat(remandStatusChangedEventRule.shouldExecute(), is(false));
    }

    @Test
    public void defendantsShouldRepeatOncePerEmailWhenBailStatusChanged() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        when(hearingService.getHearing(HEARING_ID)).thenReturn(of(buildCurrentHearing()));
        final RemandStatusChangedEventRule remandStatusChangedEventRule =
                new RemandStatusChangedEventRule(buildResultedHearing(),
                        buildCaseWithBailStatus(),
                        buildSubscription(),
                        hearingService);
        remandStatusChangedEventRule.setApplicationParameters(applicationParameters);
        assertThat(remandStatusChangedEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = remandStatusChangedEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - remand status changed"));
        assertThat(emailInfo.getTitle(), is("Remand status changed"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("John SMITH. Unconditional Bail. Maggie SMITH - 1 April 1982. Unconditional Bail. ")));
    }





    private Subscription buildSubscription() {
        return subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
    }

    private Hearing buildResultedHearing() {
        return hearing()
                .withId(HEARING_ID)
                .build();
    }

    private Hearing buildCurrentHearing() {
        return hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(asList(prosecutionCase()
                        .withId(CASE_ID)
                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                        .withDefendants(asList(defendant()
                                        .withId(DEFENDANT_1)
                                        .withPersonDefendant(
                                                personDefendant()
                                                        .withPersonDetails(
                                                                person()
                                                                        .withFirstName("John")
                                                                        .withLastName("Smith")
                                                                        .build())
                                                        .withBailStatus(bailStatus().withCode("B")
                                                                .withDescription("Conditional Bail")
                                                                .build())
                                                        .build()
                                        )
                                        .build()
                                , defendant()
                                        .withId(DEFENDANT_2)
                                        .withPersonDefendant(
                                                personDefendant()
                                                        .withPersonDetails(
                                                                person()
                                                                        .withFirstName("Maggie")
                                                                        .withLastName("Smith")
                                                                        .withDateOfBirth(LocalDate.of(1982, 4, 1))
                                                                        .build())
                                                        .withBailStatus(bailStatus()
                                                                .withCode("C")
                                                                .withDescription("Custody")
                                                                .build())
                                                        .build())
                                        .build())
                        ).build()))
                .build();
    }



    private ProsecutionCase buildCaseWithBailStatus() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withId(DEFENDANT_1)
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("John")
                                                                .withLastName("Smith")
                                                                //.withDateOfBirth(LocalDate.of(1982, 4, 29))
                                                                .build())
                                                .withBailStatus(bailStatus()
                                                        .withCode("U")
                                                        .withDescription("Unconditional Bail")
                                                        .build())
                                                .build())
                                .build()

                        , defendant()
                                .withId(DEFENDANT_2)
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 1))
                                                                .build())
                                                .withBailStatus(bailStatus()
                                                        .withCode("U")
                                                        .withDescription("Unconditional Bail")
                                                        .build())
                                                .build())
                                .build(),
                        defendant()
                                .withId(DEFENDANT_3)
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggiex")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 1))
                                                                .build())
                                                .withBailStatus(bailStatus()
                                                        .withCode("U")
                                                        .withDescription("Unconditional Bail")
                                                        .build())
                                                .build())
                                .build(),
                        defendant()
                                .withId(DEFENDANT_4)
                                .withPersonDefendant(
                                        personDefendant()
                                                .withPersonDetails(
                                                        person()
                                                                .withFirstName("Maggie")
                                                                .withLastName("Smith")
                                                                .withDateOfBirth(LocalDate.of(1982, 4, 1))
                                                                .build())
                                                .build())
                                .build())
                ).build();
    }

    private ProsecutionCase buildCaseWithNoBailStatus() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withPersonDefendant(personDefendant().build())
                                .build()
                        , defendant()
                                .withPersonDefendant(personDefendant().build())
                                .build())
                ).build();
    }

    private Hearing buildSjpHearing() {
        return hearing()
                .withIsSJPHearing(true)
                .build();
    }

}
