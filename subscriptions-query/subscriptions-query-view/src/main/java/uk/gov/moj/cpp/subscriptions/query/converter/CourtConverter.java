package uk.gov.moj.cpp.subscriptions.query.converter;

import static uk.gov.moj.cpp.subscriptions.json.schemas.Court.court;

import uk.gov.moj.cpp.subscriptions.json.schemas.Court;

public class CourtConverter implements Converter<uk.gov.moj.cpp.subscriptions.persistence.entity.Court, Court> {

    @Override
    public Court convert(uk.gov.moj.cpp.subscriptions.persistence.entity.Court court) {

        return court()
                .withId(court.getId())
                .withName(court.getName())
                .withCourtId(court.getCourtId())
                .build();
    }
}
