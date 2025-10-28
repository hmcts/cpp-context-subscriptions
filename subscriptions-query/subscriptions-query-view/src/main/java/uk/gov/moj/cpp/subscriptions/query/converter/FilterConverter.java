package uk.gov.moj.cpp.subscriptions.query.converter;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Gender.*;

import uk.gov.moj.cpp.subscriptions.json.schemas.Filter;
import uk.gov.moj.cpp.subscriptions.json.schemas.FilterType;

public class FilterConverter implements Converter<uk.gov.moj.cpp.subscriptions.persistence.entity.Filter, Filter> {

    final DefendantConverter defendantConverter = new DefendantConverter();

    @Override
    public Filter convert(uk.gov.moj.cpp.subscriptions.persistence.entity.Filter filter) {
        return filter()
                .withId(filter.getId())
                .withUrn(filter.getUrn())
                .withOffence(filter.getOffence())
                .withGender(ofNullable(filter.getGender()).map(a -> valueOf(filter.getGender().name())).orElse(null))
                .withIsAdult(filter.isAdult())
                .withFilterType(FilterType.valueOf(filter.getFilterType().name()))
                .withDefendant(defendantConverter.convert(filter))
                .build();

    }
}
