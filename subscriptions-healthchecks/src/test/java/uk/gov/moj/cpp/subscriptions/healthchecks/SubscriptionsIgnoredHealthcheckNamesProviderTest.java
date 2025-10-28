package uk.gov.moj.cpp.subscriptions.healthchecks;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.healthcheck.healthchecks.FileStoreHealthcheck.FILE_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionsIgnoredHealthcheckNamesProviderTest {


    @InjectMocks
    private SubscriptionsIgnoredHealthcheckNamesProvider subscriptionsIgnoredHealthcheckNamesProvider;

    @Test
    public void shouldIgnoreFileStoreAndJobStoreHealthchecks() throws Exception {

        final List<String> namesOfIgnoredHealthChecks = subscriptionsIgnoredHealthcheckNamesProvider.getNamesOfIgnoredHealthChecks();

        assertThat(namesOfIgnoredHealthChecks.size(), is(2));
        assertThat(namesOfIgnoredHealthChecks, hasItems(JOB_STORE_HEALTHCHECK_NAME));
        assertThat(namesOfIgnoredHealthChecks, hasItems(FILE_STORE_HEALTHCHECK_NAME));
    }

}