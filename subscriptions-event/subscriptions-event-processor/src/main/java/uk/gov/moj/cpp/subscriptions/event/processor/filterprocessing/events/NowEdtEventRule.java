package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.time.LocalDate;
import java.util.UUID;


public class NowEdtEventRule extends AbstractEventRule {

    private NowDocumentContent nowDocumentContent;
    private ProsecutionCase prosecutionCase;
    private Nowdefendant defendant;
    private UUID materialId;
    private String nowEdtName;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;

    public NowEdtEventRule(final NowDocumentContent nowDocumentContent,
                           final ProsecutionCase prosecutionCase,
                           final UUID materialId,
                           final String nowEdtName,
                           final Subscription subscription,
                           final ApplicationParameters applicationParameters
    ) {
        this.nowDocumentContent = nowDocumentContent;
        this.prosecutionCase = prosecutionCase;
        this.defendant = nowDocumentContent.getDefendant();
        this.materialId = materialId;
        this.nowEdtName = nowEdtName;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
        this.applicationParameters = applicationParameters;
    }

    @Override
    public boolean shouldExecute() {
        return nowDocumentContent.getOrderName().equalsIgnoreCase(nowEdtName)
                && nonNull(nowDocumentContent.getCases())
                && nonNull(defendant)
                && filterStrategy.caseMatches(buildProsecutionCaseFromNowContent());
    }

    private uk.gov.justice.core.courts.ProsecutionCase buildProsecutionCaseFromNowContent() {
       final Defendant.Builder defendantBuilder = defendant().withIsYouth("Y".equalsIgnoreCase(defendant.getIsYouth()))

                .withOffences(nowDocumentContent.getCases()
                        .stream()
                        .flatMap(a -> a.getDefendantCaseOffences().stream())
                        .map(a -> offence().withOffenceCode(a.getCode()).withOffenceTitle(a.getTitle()).build())
                        .collect(toList()));
            if(nonNull(defendant.getFirstName())) {
                defendantBuilder.withPersonDefendant(personDefendant()
                                .withPersonDetails(person()
                                        .withFirstName(defendant.getFirstName())
                                        .withLastName(defendant.getLastName())
                                        .withGender(Gender.valueFor(defendant.getGender()).orElse(null))
                                        .withDateOfBirth(nonNull(defendant.getDateOfBirth()) ? LocalDate.parse(defendant.getDateOfBirth()): null)
                                        .build())
                                .build());
            } else {
                defendantBuilder.withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName(defendant.getName())
                                .build())
                        .build());

            }


        return
                prosecutionCase()
                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                .withCaseURN(prosecutionCase.getReference()).build())
                        .withDefendants(asList(defendantBuilder
                                .withOffences(nowDocumentContent.getCases()
                                        .stream()
                                        .flatMap(a -> a.getDefendantCaseOffences().stream())
                                        .map(a -> offence().withOffenceCode(a.getCode()).withOffenceTitle(a.getTitle()).build())
                                        .collect(toList()))
                                .build()
                        )).build();

    }


    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    @Override
    public EmailInfo execute() {
        return emailInfo()
                .withTitle(prepareTitle())
                .withSubject(prepareSubject())
                .withBody(prepareBody())
                .withCaseLink(prepareCaseLink())
                .withSubscription(getSubscription())
                .withMaterialId(materialId.toString())
                .withEmailTemplateId(applicationParameters.getThirdPartySubscriptionNowsEdtsTemplateId())
                .build();
    }

    @Override
    protected String prepareSubject() {
        if(nonNull(defendant.getFirstName())) {
            if(nonNull(defendant.getDateOfBirth())) {
                return format("Case {0} {1} {2} {3} - {4}",
                        prosecutionCase.getReference(),
                        defendant.getFirstName(),
                        defendant.getLastName().toUpperCase(),
                        formatDateOfBirth(LocalDate.parse(defendant.getDateOfBirth())),
                        nowEdtName);
            } else {
                return format("Case {0} {1} {2} - {3}",
                        prosecutionCase.getReference(),
                        defendant.getFirstName(),
                        defendant.getLastName().toUpperCase(),
                        nowEdtName);
            }

        } else {
            return format("Case {0} {1} - {2}", prosecutionCase.getReference(), defendant.getName(), nowEdtName);
        }
    }

    @Override
    protected String prepareTitle() {
        return "";
    }

    @Override
    protected String prepareCaseLink() {
        return "";
    }

    @Override
    protected String prepareBody() {
        return format("{0}", nowEdtName);
    }

}
