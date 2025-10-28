package uk.gov.moj.cpp.subscriptions.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.subscriptions.command.api.service.UserAndGroupsService;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriberCommandApiTest {

    @Mock
    private Sender sender;


    @InjectMocks
    private SubscriberCommandApi subscriberCommandApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> jsonEnvelopeArgumentCaptor;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    private final JsonEnvelope command = mock(JsonEnvelope.class);

    @Test
    public void shouldHandleSubscribe() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.subscribe");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "org1234").add("email", "test@test.com").build());
        subscriberCommandApi.subscribe(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.subscribe"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("org1234")),
                withJsonPath("$.subscriber", equalTo("test@test.com"))
        )));
    }

    @Test
    public void shouldThrowExceptionWhenSubscribeByUserWithNoOrganisation() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.subscribe");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("email", "test@test.com").build());
        assertThrows(BadRequestException.class, () -> subscriberCommandApi.subscribe(command));
    }

    @Test
    public void shouldHandleUnsubscribe() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.unsubscribe");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "org1234").add("email", "test@test.com").build());
        subscriberCommandApi.unsubscribe(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.unsubscribe"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("org1234")),
                withJsonPath("$.subscriber", equalTo("test@test.com"))
        )));
    }


    @Test
    public void shouldHandleDeleteSubscribe() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.delete-subscriber");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "org1234").add("email", "test@test.com").build());
        subscriberCommandApi.deleteSubscribe(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.delete-subscriber"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("org1234")),
                withJsonPath("$.subscriber", equalTo("test@test.com"))
        )));
    }
}