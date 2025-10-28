package uk.gov.moj.cpp.subscriptions.persistence.constants;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.CHANGE_OF_PLEA;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.CRACKED_OR_INEFFECTIVE_TRAIL;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.DEFENDANT_APPELLANT_ATTENDANCE;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.PLEAS_ENTER;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.PRE_SENTENCE_REPORT_REQUESTED;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.REMAND_STATUS;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.VERDICTS_ENTER;
import static uk.gov.moj.cpp.subscriptions.persistence.constants.EventType.eventByValue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EventTypeTest {

    public static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(CRACKED_OR_INEFFECTIVE_TRAIL),
                Arguments.of(CHANGE_OF_PLEA),
                Arguments.of(REMAND_STATUS),
                Arguments.of(DEFENDANT_APPELLANT_ATTENDANCE),
                Arguments.of(PLEAS_ENTER),
                Arguments.of(VERDICTS_ENTER),
                Arguments.of(PRE_SENTENCE_REPORT_REQUESTED)
        );
    }

    @Test
    public void shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> eventByValue("Test Value"));
    }

    @MethodSource("events")
    @ParameterizedTest
    public void shouldGetRightEnumWhenSentAValue(final EventType event) {
        assertThat(eventByValue(event.getValue()), is(event));
    }
}