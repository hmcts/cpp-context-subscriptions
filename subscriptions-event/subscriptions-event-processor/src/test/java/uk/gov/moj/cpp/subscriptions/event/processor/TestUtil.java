package uk.gov.moj.cpp.subscriptions.event.processor;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.Charset;

import com.google.common.io.Resources;

public class TestUtil {

    public static String readFile(String filePath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(filePath),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            fail("Error consuming file from location " + filePath);
        }
        return request;
    }
}
