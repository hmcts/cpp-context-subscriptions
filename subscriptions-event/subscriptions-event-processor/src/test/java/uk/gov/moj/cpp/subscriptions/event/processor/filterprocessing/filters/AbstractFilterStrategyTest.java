package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters;


import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Filter.filter;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.AGE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.CASE_REFERENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.DEFENDANT;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.GENDER;
import static uk.gov.moj.cpp.subscriptions.json.schemas.FilterType.OFFENCE;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class AbstractFilterStrategyTest {

    @Test
    public void shouldCreateDefendantFilter() {
        final AbstractFilterStrategy filter = createFilter(subscription().withFilter(filter().withFilterType(DEFENDANT).build()).build());
        assertThat(filter, Matchers.instanceOf(DefendantFilterStrategy.class));
    }

    @Test
    public void shouldCreateCaseReferenceFilter() {
        final AbstractFilterStrategy filter = createFilter(subscription().withFilter(filter().withFilterType(CASE_REFERENCE).build()).build());
        assertThat(filter, Matchers.instanceOf(UrnFilterStrategy.class));
    }

    @Test
    public void shouldCreateGenderFilter() {
        final AbstractFilterStrategy filter = createFilter(subscription().withFilter(filter().withFilterType(GENDER).build()).build());
        assertThat(filter, Matchers.instanceOf(GenderFilterStrategy.class));
    }

    @Test
    public void shouldCreateOffenceFilter() {
        final AbstractFilterStrategy filter = createFilter(subscription().withFilter(filter().withFilterType(OFFENCE).build()).build());
        assertThat(filter, Matchers.instanceOf(OffenceFilterStrategy.class));
    }

    @Test
    public void shouldCreateAgeFilter() {
        final AbstractFilterStrategy filter = createFilter(subscription().withFilter(filter().withFilterType(AGE).build()).build());
        assertThat(filter, Matchers.instanceOf(AgeFilterStrategy.class));
    }

}
