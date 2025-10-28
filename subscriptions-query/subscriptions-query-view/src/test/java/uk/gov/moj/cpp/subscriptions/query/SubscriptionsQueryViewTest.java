package uk.gov.moj.cpp.subscriptions.query;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType.CASE_REFERENCE;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Filter;
import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;
import uk.gov.moj.cpp.subscriptions.persistence.repository.SubscriptionsRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionsQueryViewTest {

    @Mock
    private SubscriptionsRepository subscriptionsRepository;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @InjectMocks
    private SubscriptionsQueryView subscriptionsQueryView;

    @Test
    public void shouldRetrieveSubscriptions() {

        when(subscriptionsRepository.findByOrganisationId(any()))
                .thenReturn(asList(Subscription.builder()
                        .withName("ABCD")
                        .withFilters(Filter.builder()
                                .withFilterType(CASE_REFERENCE)
                                .withUrn("URN123")
                                .withId(randomUUID())
                                .build())
                        .build()));

        final JsonEnvelope jsonEnvelope = subscriptionsQueryView.retrieveSubscriptions(this.jsonEnvelope, randomUUID());
        assertThat(jsonEnvelope.payloadAsJsonObject().toString(), isJson(withJsonPath("$.subscriptions[0].name", equalTo("ABCD"))));
    }

    @Test
    public void shouldRetrieveSubscriptionsByCourtId() {

        when(subscriptionsRepository.findByCourtId(any()))
                .thenReturn(asList(Subscription.builder()
                        .withName("ABCD")
                        .withFilters(Filter.builder()
                                .withFilterType(CASE_REFERENCE)
                                .withUrn("URN123")
                                .withId(randomUUID())
                                .build())
                        .build()));

        final JsonEnvelope jsonEnvelope = subscriptionsQueryView.retrieveSubscriptionsByCourtId(this.jsonEnvelope, randomUUID());
        assertThat(jsonEnvelope.payloadAsJsonObject().toString(), isJson(withJsonPath("$.subscriptions[0].name", equalTo("ABCD"))));
    }

    @Test
    public void shouldRetrieveSubscriptionsBySubscriber() {

        when(subscriptionsRepository.findByOrganisationIdAndSubscriber(any(), anyString()))
                .thenReturn(asList(Subscription.builder()
                        .withName("ABCD")
                        .withFilters(Filter.builder()
                                .withFilterType(CASE_REFERENCE)
                                .withUrn("URN123")
                                .withId(randomUUID())
                                .build())
                        .build()));

        final JsonEnvelope jsonEnvelope = subscriptionsQueryView.retrieveSubscriptions(this.jsonEnvelope, randomUUID(), "test@test.com");
        assertThat(jsonEnvelope.payloadAsJsonObject().toString(), isJson(withJsonPath("$.subscriptions[0].name", equalTo("ABCD"))));
    }

}
