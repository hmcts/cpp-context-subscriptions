package uk.gov.moj.cpp.subscriptions.event.processor.service;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;

import java.util.UUID;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "thirdPartySubscriptionTemplateId", defaultValue = "d3041cbf-fea3-4c45-9316-eb57d1b2996b")
    private String thirdPartySubscriptionTemplateId;

    @Inject
    @Value(key = "thirdPartySubscriptionNowsEdtsTemplateId", defaultValue = "09f8a4c2-96f1-405d-8fb2-047ed456448c")
    private String thirdPartySubscriptionNowsEdtsTemplateId;

    @Inject
    @Value(key = "cppAppUrl", defaultValue = "http://localhost:8080/")
    private String cppAppUrl;

    @Inject
    @Value(key = "caseAtaGlanceURI", defaultValue = "prosecution-casefile/case-at-a-glance/")
    private String caseAtaGlanceURI;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    public String getCppAppUrl() {
        return cppAppUrl;
    }

    public String getCaseAtaGlanceURI() {
        return caseAtaGlanceURI;
    }

    public String getThirdPartySubscriptionTemplateId() {
        return thirdPartySubscriptionTemplateId;
    }

    public String getMaterialUrl(final UUID materialId) {
        return materialUrlGenerator.pdfFileStreamUrlFor(materialId);
    }

    public String getThirdPartySubscriptionNowsEdtsTemplateId() {
        return thirdPartySubscriptionNowsEdtsTemplateId;
    }
}
