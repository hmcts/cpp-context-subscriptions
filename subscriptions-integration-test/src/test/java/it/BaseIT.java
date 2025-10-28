package it;

import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;

@ExtendWith(JmsResourceManagementExtension.class)
public abstract class BaseIT {

    public static final String CONTEXT_NAME = "subscriptions";
}
