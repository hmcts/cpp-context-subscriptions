package uk.gov.moj.cpp.subscriptions.event.processor.events;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CrackedIneffectiveTrial.crackedIneffectiveTrial;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.event.processor.TestUtil.readFile;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.TrialEffectivenessEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TrialEffectivenessEventRuleTest {

    private static final UUID CASE_ID = randomUUID();

    @Mock
    private ApplicationParameters applicationParameters;

    @Test
    public void shouldNotExecuteWhenNoTrialEffectivenessIsSet() {
        final Subscription subscription = buildSubscription();
        final TrialEffectivenessEventRule pleaEnteredEventRule = new TrialEffectivenessEventRule(buildHearingWithNoEffectiveTrialIsSet(), null, subscription);
        assertThat(pleaEnteredEventRule.shouldExecute(), is(false));
    }

    @Test
    public void shouldNotExecuteWhenProsecutionAuthorityReferenceIsSetInsteadOfCaseUrn() {
        final ProsecutionCase prosecutionCase = buildCaseWithAuthorityReference();

        final Subscription subscription = buildSubscription();
        final TrialEffectivenessEventRule trialEffectivenessEventRule = new TrialEffectivenessEventRule(buildHearing(), prosecutionCase, subscription);
        assertThat(trialEffectivenessEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void shouldExecuteWhenCrackedIneffectiveTrialIsSet() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = buildCase();

        final Subscription subscription = buildSubscription();
        final TrialEffectivenessEventRule trialEffectivenessEventRule = new TrialEffectivenessEventRule(buildHearing(), prosecutionCase, subscription);
        assertThat(trialEffectivenessEventRule.shouldExecute(), equalTo(true));
        trialEffectivenessEventRule.setApplicationParameters(applicationParameters);
        final EmailInfo emailInfo = trialEffectivenessEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - trial effectiveness set"));
        final String body = readFile("email-trial-effectiveness-set.txt")
                .replace("%CASE_ID%", CASE_ID.toString())
                .replace("%TYPE%", "Cracked");
        assertThat(emailInfo.getBody(), is("Trial for case URN123 is Cracked. "));
    }

    @Test
    public void shouldExecuteWhenIsEffectiveTrialIsTrue() {
        when(applicationParameters.getCaseAtaGlanceURI()).thenReturn("prosecution-casefile/case-at-a-glance/");
        when(applicationParameters.getCppAppUrl()).thenReturn("http://localhost:8080/");
        final ProsecutionCase prosecutionCase = buildCase();

        final Subscription subscription = buildSubscription();
        final TrialEffectivenessEventRule trialEffectivenessEventRule = new TrialEffectivenessEventRule(buildHearingWithIsEffectiveTrialTrue(), prosecutionCase, subscription);
        assertThat(trialEffectivenessEventRule.shouldExecute(), equalTo(true));
        trialEffectivenessEventRule.setApplicationParameters(applicationParameters);
        final EmailInfo emailInfo = trialEffectivenessEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 - trial effectiveness set"));
        final String body = readFile("email-trial-effectiveness-set.txt")
                .replace("%CASE_ID%", CASE_ID.toString())
                .replace("%TYPE%", "Effective");
        assertThat(emailInfo.getBody(), is("Trial for case URN123 is Effective. "));
    }

    private Subscription buildSubscription() {
        return Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
    }

    private ProsecutionCase buildCase() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withCaseURN("URN123")
                        .build())
                .build();
    }

    private ProsecutionCase buildCaseWithAuthorityReference() {
        return prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("REF123")
                        .build())
                .build();
    }


    private Hearing buildHearing() {
        return hearing()
                .withCrackedIneffectiveTrial(crackedIneffectiveTrial()
                        .withType("Cracked")
                        .build())
                .build();
    }

    private Hearing buildHearingWithNoEffectiveTrialIsSet() {
        return hearing()
                .build();
    }

    private Hearing buildHearingWithIsEffectiveTrialTrue() {
        return hearing()
                .withIsEffectiveTrial(true)
                .build();
    }
}
