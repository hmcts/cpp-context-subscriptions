package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.text.MessageFormat.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo.emailInfo;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;

import java.util.List;
import java.util.UUID;

public abstract class HearingEventRule extends AbstractEventRule {

    private static final String LINE_SEPARATOR = ". ";

    protected abstract UUID getCaseId();

    protected abstract String getCaseUrn();

    protected abstract String getTitle();

    protected abstract List<Section> getSections();

    @Override
    public EmailInfo execute() {
        return emailInfo()
                .withTitle(prepareTitle())
                .withSubject(prepareSubject())
                .withBody(prepareBody())
                .withCaseLink(prepareCaseLink())
                .withSubscription(getSubscription())
                .withEmailTemplateId(applicationParameters.getThirdPartySubscriptionTemplateId())
                .build();
    }

    @Override
    protected String prepareSubject() {
        return format("Case {0} - {1}", getCaseUrn(), getTitle().toLowerCase());
    }

    protected String prepareTitle() {
        return format("{0}", getTitle());
    }

    protected String prepareCaseLink() {
        return format("{0}", buildCaseAccessInfo());
    }

    @Override
    protected String prepareBody() {
        List<Section> sections = getSections();
        String linesBuilder = sections.stream()
                .flatMap(section -> section.getLines().stream())
                .collect(joining(LINE_SEPARATOR, "", LINE_SEPARATOR));

        return format("{0}", linesBuilder);
    }

    protected String prepareDefendantLine(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            return preparePersonalDefendantInfo(defendant);
        } else {
            return defendant.getLegalEntityDefendant().getOrganisation().getName();
        }
    }

    private String buildCaseAccessInfo() {
        return format("Access the case {0} for full details.",
                applicationParameters.getCppAppUrl()
                        + applicationParameters.getCaseAtaGlanceURI()
                        + getCaseId());
    }


}
