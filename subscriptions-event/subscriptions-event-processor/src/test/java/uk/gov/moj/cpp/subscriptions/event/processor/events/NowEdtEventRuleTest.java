package uk.gov.moj.cpp.subscriptions.event.processor.events;


import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.nowdocument.DefendantCaseOffence.defendantCaseOffence;
import static uk.gov.justice.core.courts.nowdocument.NowDocumentContent.nowDocumentContent;
import static uk.gov.justice.core.courts.nowdocument.Nowdefendant.nowdefendant;
import static uk.gov.justice.core.courts.nowdocument.ProsecutionCase.prosecutionCase;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.AGE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.DEFENDANT;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.GENDER;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.OFFENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Gender.MALE;

import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.NowEdtEventRule;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.Defendant;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowEdtEventRuleTest {

    @InjectMocks
    private ApplicationParameters applicationParameters;

    @Test
    public void shouldNotExecuteWhenDefendantsNotPresentInPublicEvent() {
        final Subscription subscription = createCommonSubscription();
        final ProsecutionCase prosecutionCase = createProsecutionCase();

        final String nowEdtName = "Admission to hospital on committal to Crown Court";

        final NowDocumentContent nowDocumentContent = nowDocumentContent()
                .withOrderName(nowEdtName)
                .withCases(asList(prosecutionCase))
                .build();

        final UUID materialId = randomUUID();

        final NowEdtEventRule nowEdtEventRule = new NowEdtEventRule(nowDocumentContent,
                prosecutionCase,
                materialId,
                nowEdtName,
                subscription,
                applicationParameters);

        assertThat(nowEdtEventRule.shouldExecute(), equalTo(false));
    }

    @Test
    public void shouldNotExecuteWhenCasesNotPresentInPublicEvent() {
        final Subscription subscription = createCommonSubscription();

        final ProsecutionCase prosecutionCase = createProsecutionCase();

        final String nowEdtName = "Admission to hospital on committal to Crown Court";

        final NowDocumentContent nowDocumentContent = nowDocumentContent()
                .withOrderName(nowEdtName)
                .withCases(null)
                .build();

        final UUID materialId = randomUUID();

        final NowEdtEventRule nowEdtEventRule = new NowEdtEventRule(nowDocumentContent,
                prosecutionCase,
                materialId,
                nowEdtName,
                subscription,
                applicationParameters);

        assertThat(nowEdtEventRule.shouldExecute(), equalTo(false));

    }

    @Test
    public void shouldExecuteWithCaseReferenceFilter() {
        final Subscription subscription = createCommonSubscription();

        executeWithSubscription(subscription);
    }

    @Test
    public void shouldExecuteWithCaseReferenceFilterForLegalEntityDefendant() {
        final Subscription subscription = createCommonSubscription();

        executeWithSubscriptionForLegalEntityDefendant(subscription);
    }



    @Test
    public void shouldExecuteWithOffenceFilter() {
        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(OFFENCE)
                        .withOffence("OffenceCode1")
                        .build()).build();

        executeWithSubscription(subscription);
    }

    @Test
    public void shouldExecuteWithOffenceFilterForLegalEntityDefendant() {
        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(OFFENCE)
                        .withOffence("OffenceCode1")
                        .build()).build();

        executeWithSubscriptionForLegalEntityDefendant(subscription);
    }

    @Test
    public void shouldExecuteWithDefendantFilter() {
        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(DEFENDANT)
                        .withDefendant(Defendant.defendant()
                                .withFirstName("John")
                                .withLastName("Smith")
                                .withDateOfBirth(LocalDate.parse("1972-07-01"))
                                .build()).build()).build();

        executeWithSubscription(subscription);
    }

    @Test
    public void shouldNotExecuteWithLegalEntityDefendantFilter() {
        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(DEFENDANT)
                        .withDefendant(Defendant.defendant()
                                .withFirstName("John")
                                .withLastName("Smith")
                                .withDateOfBirth(LocalDate.parse("1972-07-01"))
                                .build()).build()).build();

        shouldNotexecuteWithSubscriptionForLegalEntityDefendant(subscription);
    }



    @Test
    public void shouldExecuteWithGenderFilter() {
        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(GENDER)
                        .withGender(MALE)
                        .build()).build();

        executeWithSubscription(subscription);
    }

    @Test
    public void shouldExecuteWithAgeFilter() {
        final Subscription subscription = Subscription
                .subscription()
                .withFilter(filter().withFilterType(AGE)
                        .withIsAdult(false)
                        .build()).build();

        executeWithSubscription(subscription);
    }

    private Subscription createCommonSubscription() {
        return Subscription
                .subscription()
                .withFilter(filter().withFilterType(CASE_REFERENCE)
                        .withUrn("URN123")
                        .build()).build();
    }

    private ProsecutionCase createProsecutionCase() {
        return prosecutionCase()
                .withReference("URN123")
                .withDefendantCaseOffences(asList(defendantCaseOffence()
                        .withCode("OffenceCode1")
                        .withTitle("Offence Title")
                        .build()))
                .build();
    }

    private void executeWithSubscription(final Subscription subscription) {
        final Nowdefendant nowdefendant = nowdefendant()
                .withFirstName("John")
                .withLastName("Smith")
                .withDateOfBirth("1972-07-01")
                .withGender("MALE")
                .withIsYouth("y")
                .build();

        final ProsecutionCase prosecutionCase = createProsecutionCase();

        final String nowEdtName = "Admission to hospital on committal to Crown Court";

        final NowDocumentContent nowDocumentContent = nowDocumentContent()
                .withDefendant(nowdefendant)
                .withOrderName(nowEdtName)
                .withCases(asList(prosecutionCase))
                .build();


        final UUID materialId = randomUUID();

        final NowEdtEventRule nowEdtEventRule = new NowEdtEventRule(nowDocumentContent,
                prosecutionCase,
                materialId,
                nowEdtName,
                subscription,
                applicationParameters);

        assertThat(nowEdtEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = nowEdtEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 John SMITH 1 July 1972 - Admission to hospital on committal to Crown Court"));
        assertThat(emailInfo.getBody(), is("Admission to hospital on committal to Crown Court"));
        assertThat(emailInfo.getMaterialId(), is(materialId.toString()));
    }

    private void executeWithSubscriptionForLegalEntityDefendant(final Subscription subscription) {
        final Nowdefendant nowdefendant = nowdefendant()
                .withName("ABC Corporation")
                .build();

        final ProsecutionCase prosecutionCase = createProsecutionCase();

        final String nowEdtName = "Admission to hospital on committal to Crown Court";

        final NowDocumentContent nowDocumentContent = nowDocumentContent()
                .withDefendant(nowdefendant)
                .withOrderName(nowEdtName)
                .withCases(asList(prosecutionCase))
                .build();


        final UUID materialId = randomUUID();

        final NowEdtEventRule nowEdtEventRule = new NowEdtEventRule(nowDocumentContent,
                prosecutionCase,
                materialId,
                nowEdtName,
                subscription,
                applicationParameters);

        assertThat(nowEdtEventRule.shouldExecute(), equalTo(true));
        final EmailInfo emailInfo = nowEdtEventRule.execute();
        assertThat(emailInfo.getSubject(), is("Case URN123 ABC Corporation - Admission to hospital on committal to Crown Court"));
        assertThat(emailInfo.getBody(), is("Admission to hospital on committal to Crown Court"));
        assertThat(emailInfo.getMaterialId(), is(materialId.toString()));
    }


    private void shouldNotexecuteWithSubscriptionForLegalEntityDefendant(final Subscription subscription) {
        final Nowdefendant nowdefendant = nowdefendant()
                .withName("ABC Corporation")
                .build();

        final ProsecutionCase prosecutionCase = createProsecutionCase();

        final String nowEdtName = "Admission to hospital on committal to Crown Court";

        final NowDocumentContent nowDocumentContent = nowDocumentContent()
                .withDefendant(nowdefendant)
                .withOrderName(nowEdtName)
                .withCases(asList(prosecutionCase))
                .build();


        final UUID materialId = randomUUID();

        final NowEdtEventRule nowEdtEventRule = new NowEdtEventRule(nowDocumentContent,
                prosecutionCase,
                materialId,
                nowEdtName,
                subscription,
                applicationParameters);

        assertThat(nowEdtEventRule.shouldExecute(), equalTo(false));

    }
}
