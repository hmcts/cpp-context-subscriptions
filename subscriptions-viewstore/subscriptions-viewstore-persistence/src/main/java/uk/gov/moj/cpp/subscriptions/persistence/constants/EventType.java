package uk.gov.moj.cpp.subscriptions.persistence.constants;

import java.util.Arrays;

public enum EventType {
    CRACKED_OR_INEFFECTIVE_TRAIL("Cracked or ineffective trail"),
    CHANGE_OF_PLEA("Change of plea"),
    REMAND_STATUS("Remand Status"),
    DEFENDANT_APPELLANT_ATTENDANCE("Defendant appellant attendance"),
    PLEAS_ENTER("Pleas enter"),
    VERDICTS_ENTER("Verdicts enter"),
    PRE_SENTENCE_REPORT_REQUESTED("Pre Sentence report requested");

    private String value;

    EventType(final String value) {
        this.value = value;
    }

    public static EventType eventByValue(final String value) {
        return  Arrays.stream(values()).filter(e -> e.value.equals(value)).findFirst().orElseThrow(IllegalArgumentException::new);
    }

    public String getValue() {
        return value;
    }
}
