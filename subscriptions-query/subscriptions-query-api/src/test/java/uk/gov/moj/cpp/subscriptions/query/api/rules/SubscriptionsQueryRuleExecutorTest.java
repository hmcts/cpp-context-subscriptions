package uk.gov.moj.cpp.subscriptions.query.api.rules;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.subscriptions.query.api.accesscontrol.PermissionConstants.getSubscriberPermission;
import static uk.gov.moj.cpp.subscriptions.query.api.accesscontrol.PermissionConstants.getSubscriptionsPermission;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class SubscriptionsQueryRuleExecutorTest extends BaseDroolsAccessControlTest {

    @Mock
    protected UserAndGroupProvider userAndGroupProvider;

    protected Action action;

    public SubscriptionsQueryRuleExecutorTest() {
        super("QUERY_API_SESSION");
    }


    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    public static Stream<Arguments> actions() throws JsonProcessingException {
        return Stream.of(
                Arguments.of(new Actions("subscriptions.query.subscriptions", getSubscriptionsPermission())),
                Arguments.of(new Actions("subscriptions.query.subscriptions-by-user", getSubscriberPermission()))
        );
    }

    @MethodSource("actions")
    @ParameterizedTest
    public void shouldAllowUserToAccessTheAction(final Actions actions) {
        action = createActionFor(actions.getAction());
        when(userAndGroupProvider.hasPermission(action, actions.getAccess())).thenReturn(true);
        final ExecutionResults executionResults = executeRulesWith(action);
        assertSuccessfulOutcome(executionResults);
        verify(userAndGroupProvider).hasPermission(action, actions.getAccess());
        verifyNoMoreInteractions(userAndGroupProvider);
    }

    @MethodSource("actions")
    @ParameterizedTest
    public void shouldNotAllowUserToAccessTheAction(final Actions actions) {
        action = createActionFor(actions.getAction());
        when(userAndGroupProvider.hasPermission(action, actions.getAccess())).thenReturn(false);
        final ExecutionResults executionResults = executeRulesWith(action);
        assertFailureOutcome(executionResults);
        verify(userAndGroupProvider).hasPermission(action, actions.getAccess());
        verifyNoMoreInteractions(userAndGroupProvider);
    }


    public static class Actions {
        private final String action;
        private final String access;

        public Actions(final String action, final String access) {
            this.action = action;
            this.access = access;
        }

        public String getAction() {
            return action;
        }

        public String getAccess() {
            return access;
        }
    }
}
