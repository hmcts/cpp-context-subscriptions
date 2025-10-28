package uk.gov.moj.cpp.subscriptions.event.processor.events;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.AttendanceDay.attendanceDay;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantAttendance.defendantAttendance;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.Plea.plea;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.AttendanceType;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.DefendantPresentEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantPresentEventRuleTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String LINE_SEPARATOR = ". ";


    @Mock
    private ApplicationParameters applicationParameters;

    @Test
    public void defendantsShouldBeRepeatedPerOffence() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final Hearing hearing = buildHearing(defendantId1, defendantId2);
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffences(defendantId1, defendantId2);

        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final DefendantPresentEventRule defendantPresentEventRule = new DefendantPresentEventRule(hearing, prosecutionCase, subscription);
        defendantPresentEventRule.setApplicationParameters(applicationParameters);
        assertThat(defendantPresentEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = defendantPresentEventRule.execute();

        assertThat(emailInfo.getSubject(), is("Case URN123 - defendant present"));
        assertThat(emailInfo.getTitle(), is("Defendant present"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("John SMITH. In person - 12 April 2021. " +
                "By video - 27 April 2021. By video - 2 April 2021. Not present - 20 April 2021. " +
                "Maggie SMITH - 29 April 1982. In person - 12 March 2021. In person - 2 March 2021. " +
                "By video - 27 March 2021. Not present - 20 March 2021. ")));

    }


    @Test
    public void shouldNotExecuteWhenDefendantIsNull() {
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final Hearing hearing = hearing()
                .withCourtCentre(courtCentre().build())
                .withDefendantAttendance(null)
                .build();
        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffences(defendantId1, defendantId2);

        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final DefendantPresentEventRule defendantPresentEventRule = new DefendantPresentEventRule(hearing, prosecutionCase, subscription);
        defendantPresentEventRule.setApplicationParameters(applicationParameters);
        assertThat(defendantPresentEventRule.shouldExecute(), equalTo(false));

    }

    @Test
    public void shouldNotExecuteWhenDefendantIsLegalEntityDefendant() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final Hearing hearing = buildHearing(defendantId1, defendantId2);

        final ProsecutionCase prosecutionCase = createCaseWithMultipleOffencesAndLegalEntityDefendant(defendantId1, defendantId2);

        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
        final DefendantPresentEventRule defendantPresentEventRule = new DefendantPresentEventRule(hearing, prosecutionCase, subscription);
        defendantPresentEventRule.setApplicationParameters(applicationParameters);
        assertThat(defendantPresentEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = defendantPresentEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - defendant present"));
        assertThat(emailInfo.getTitle(), is("Defendant present"));
        assertThat(emailInfo.getCaseLink(), is(format("Access the case http://localhost:8080/prosecution-casefile/case-at-a-glance/{0} for full details.",CASE_ID)));
        assertThat(emailInfo.getBody(), is(format("ABC corporation. In person - 12 April 2021. " +
                "By video - 27 April 2021. By video - 2 April 2021. Not present - 20 April 2021. " +
                "Maggie SMITH - 29 April 1982. In person - 12 March 2021. In person - 2 March 2021. " +
                "By video - 27 March 2021. Not present - 20 March 2021. ")));


    }


    private Hearing buildHearing(final UUID defendantId1, final UUID defendantId2) {
        return hearing()
                .withCourtCentre(courtCentre().build())
                .withDefendantAttendance(asList(
                        defendantAttendance()
                                .withAttendanceDays(asList(
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.BY_VIDEO)
                                                .withDay(LocalDate.of(2021, 4, 2))
                                                .build(),
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.NOT_PRESENT)
                                                .withDay(LocalDate.of(2021, 4, 20))
                                                .build(),
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.IN_PERSON)
                                                .withDay(LocalDate.of(2021, 4, 12))
                                                .build(),
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.BY_VIDEO)
                                                .withDay(LocalDate.of(2021, 4, 27))
                                                .build()
                                ))
                                .withDefendantId(defendantId1)
                                .build(),
                        defendantAttendance()
                                .withAttendanceDays(asList(
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.BY_VIDEO)
                                                .withDay(LocalDate.of(2021, 3, 27))
                                                .build(),
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.NOT_PRESENT)
                                                .withDay(LocalDate.of(2021, 3, 20))
                                                .build(),
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.IN_PERSON)
                                                .withDay(LocalDate.of(2021, 3, 12))
                                                .build(),
                                        attendanceDay()
                                                .withAttendanceType(AttendanceType.IN_PERSON)
                                                .withDay(LocalDate.of(2021, 3, 2))
                                                .build()
                                ))
                                .withDefendantId(defendantId2)
                                .build()
                ))
                .build();
    }

    private ProsecutionCase createCaseWithMultipleOffences(final UUID defendantId1, final UUID defendantId2) {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withId(defendantId1)
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
                                                createOffence("Failing to report an accident", "Not guilty"),
                                                createOffence("Murder", "Guilty")
                                        )
                                )
                                .build()
                        , defendant()
                                .withId(defendantId2)
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
                                        createOffence("Driving while disqualified", "Guilty"))
                                )
                                .build())
                ).build();
    }

    private ProsecutionCase createCaseWithMultipleOffencesAndLegalEntityDefendant(final UUID defendantId1, final UUID defendantId2) {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("URN123").build())
                .withDefendants(asList(defendant()
                                .withId(defendantId1)
                                .withLegalEntityDefendant(
                                        LegalEntityDefendant.legalEntityDefendant()
                                                .withOrganisation(Organisation.organisation()
                                                        .withName("ABC corporation")
                                                        .build())
                                                .build())
                                .withOffences(asList(
                                                createOffenceWithNonPlea("An offence without plea"),
                                                createOffence("Failing to report an accident", "Not guilty"),
                                                createOffence("Murder", "Guilty")
                                        )
                                )
                                .build()
                        , defendant()
                                .withId(defendantId2)
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
                                        createOffence("Driving while disqualified", "Guilty"))
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

    private Offence createOffenceWithNonPlea(final String offenceTitle) {
        return offence()
                .withOffenceTitle(offenceTitle)
                .build();
    }

}
