package uk.gov.moj.cpp.subscriptions.query.converter;

import static uk.gov.moj.cpp.subscriptions.json.schemas.Defendant.defendant;

import uk.gov.moj.cpp.subscriptions.json.schemas.Defendant;

public class DefendantConverter implements Converter<uk.gov.moj.cpp.subscriptions.persistence.entity.Filter, Defendant> {

    @Override
    public Defendant convert(uk.gov.moj.cpp.subscriptions.persistence.entity.Filter filter) {

        return defendant()
                .withFirstName(filter.getDefendantFirstName())
                .withLastName(filter.getDefendantLastName())
                .withDateOfBirth(filter.getDateOfBirth())
                .build();
    }
}
