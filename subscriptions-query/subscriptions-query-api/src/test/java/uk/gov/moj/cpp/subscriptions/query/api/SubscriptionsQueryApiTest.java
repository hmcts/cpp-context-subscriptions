package uk.gov.moj.cpp.subscriptions.query.api;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.subscriptions.query.SubscriptionsQueryView;
import uk.gov.moj.cpp.subscriptions.query.api.service.UserAndGroupsService;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionsQueryApiTest {
    @Mock
    private SubscriptionsQueryView subscriptionsQueryView;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private SubscriptionsQueryApi subscriptionsQueryApi;

    @Test
    public void shouldHandleQuerySubscriptions() {
        final Optional<String> userId = ofNullable(randomUUID().toString());
        final UUID organisationId = randomUUID();
        when(userAndGroupsService.getUserDetails(jsonEnvelope)).thenReturn(createObjectBuilder().add("organisationId", organisationId.toString()).build());

        subscriptionsQueryApi.retrieveSubscriptions(jsonEnvelope);

        verify(userAndGroupsService).getUserDetails(jsonEnvelope);
        verify(subscriptionsQueryView).retrieveSubscriptions(jsonEnvelope, organisationId);
    }

    @Test
    public void shouldHandleQuerySubscriptionsByCourtId() {
        final UUID courtId = randomUUID();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("courtId",courtId.toString()).build());
        subscriptionsQueryApi.retrieveSubscriptionsByCourtId(jsonEnvelope);
        verify(subscriptionsQueryView).retrieveSubscriptionsByCourtId(jsonEnvelope, courtId);
    }

    @Test
    public void shouldHandleQuerySubscriptionsByUser() {
        final Optional<String> userId = ofNullable(randomUUID().toString());
        final UUID organisationId = randomUUID();
        final String email = "test@test.com";
        when(userAndGroupsService.getUserDetails(jsonEnvelope)).thenReturn(createObjectBuilder().add("organisationId", organisationId.toString()).add("email", email).build());

        subscriptionsQueryApi.retrieveSubscriptionsByUser(jsonEnvelope);

        verify(userAndGroupsService).getUserDetails(jsonEnvelope);
        verify(subscriptionsQueryView).retrieveSubscriptions(jsonEnvelope, organisationId, email);
    }
}
