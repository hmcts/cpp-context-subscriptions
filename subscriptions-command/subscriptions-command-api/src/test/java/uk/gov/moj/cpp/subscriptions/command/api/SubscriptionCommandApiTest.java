package uk.gov.moj.cpp.subscriptions.command.api;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

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
public class SubscriptionCommandApiTest {

    @Mock
    private Sender sender;


    @InjectMocks
    private SubscriptionCommandApi subscriptionCommandApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> jsonEnvelopeArgumentCaptor;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    private final JsonEnvelope command = mock(JsonEnvelope.class);


    @Test
    public void shouldHandleSubscriptionCommand() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.create-subscription-by-admin");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("name", "SubscriptionName").build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "org1234").build());
        subscriptionCommandApi.subscription(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.create-subscription-by-admin"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("org1234")),
                withJsonPath("$.name", equalTo("SubscriptionName"))
        )));
    }


    @Test
    public void shouldHandleActivateSubscriptionCommand() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.activate-subscription");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("subscriptionId", "8263c5c8-c68c-4654-9599-72ac4cd1778e").build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "8d415dd1-986b-4070-a1a9-046603ecb3cf").build());
        subscriptionCommandApi.activate(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.activate-subscription"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("8d415dd1-986b-4070-a1a9-046603ecb3cf")),
                withJsonPath("$.subscriptionId", equalTo("8263c5c8-c68c-4654-9599-72ac4cd1778e"))
        )));
    }

    @Test
    public void shouldHandleDeactivateSubscriptionCommand() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.deactivate-subscription");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("subscriptionId", "8263c5c8-c68c-4654-9599-72ac4cd1778e").build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "8d415dd1-986b-4070-a1a9-046603ecb3cf").build());
        subscriptionCommandApi.deactivate(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.deactivate-subscription"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("8d415dd1-986b-4070-a1a9-046603ecb3cf")),
                withJsonPath("$.subscriptionId", equalTo("8263c5c8-c68c-4654-9599-72ac4cd1778e"))
        )));
    }


    @Test
    public void shouldHandleDeleteSubscriptionCommand() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.delete-subscription");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("subscriptionId", "8263c5c8-c68c-4654-9599-72ac4cd1778e").build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "8d415dd1-986b-4070-a1a9-046603ecb3cf").build());
        subscriptionCommandApi.delete(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.delete-subscription"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("8d415dd1-986b-4070-a1a9-046603ecb3cf")),
                withJsonPath("$.subscriptionId", equalTo("8263c5c8-c68c-4654-9599-72ac4cd1778e"))
        )));
    }

    @Test
    public void shouldHandleSubscriptionCommandByUser() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("subscriptions.command.create-subscription-by-user");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        when(command.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("name", "SubscriptionName").build());
        when(userAndGroupsService.getUserDetails(any())).thenReturn(createObjectBuilder().add("organisationId", "org1234").add("email", "test@test.com").build());
        subscriptionCommandApi.subscriptionByUser(command);
        verify(sender).send(jsonEnvelopeArgumentCaptor.capture());
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo("subscriptions.command.handler.create-subscription-by-user"));
        assertThat(((JsonObject) jsonEnvelopeArgumentCaptor.getValue().payload()), isJson(allOf(
                withJsonPath("$.organisationId", equalTo("org1234")),
                withJsonPath("$.subscriber", equalTo("test@test.com")),
                withJsonPath("$.name", equalTo("SubscriptionName"))
        )));
    }
}
