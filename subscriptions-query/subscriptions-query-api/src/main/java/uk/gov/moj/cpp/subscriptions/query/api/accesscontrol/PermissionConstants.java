package uk.gov.moj.cpp.subscriptions.query.api.accesscontrol;

import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PermissionConstants {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private PermissionConstants() {
    }


    public static String getSubscriptionsPermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("Subscriptions")
                .withAction("Manage")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String getSubscriberPermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("Subscriber")
                .withAction("Manage")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }
}
