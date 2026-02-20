package ch.admin.bit.jeap.governance.reporting.confluence.model;

import lombok.Getter;

@Getter
public enum State {
    OK("tick", null, null, "Compliant"),
    FAIL("cross", null, null, "Non-compliant"),
    PAUSED("pause button", null, null, "Paused"),
    DISABLED("cross mark button", null, null, "Disabled"),
    UNKNOWN("question", null, null, "Unknown");

    private final String emoticon;
    private final String hipchatEmoticon;
    private final String unicode;
    private final String description;

    State(String emoticon, String hipchatEmoticon, String unicode, String description) {
        this.emoticon = emoticon;
        this.hipchatEmoticon = hipchatEmoticon;
        this.unicode = unicode;
        this.description = description;
    }
}
