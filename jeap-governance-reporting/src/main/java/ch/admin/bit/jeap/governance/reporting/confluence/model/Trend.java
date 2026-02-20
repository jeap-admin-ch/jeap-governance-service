package ch.admin.bit.jeap.governance.reporting.confluence.model;

import lombok.Getter;

@Getter
public enum Trend {
    UP(null, "prog-besser", null, "It's getting better"),
    DOWN(null, "prog-schlechter", null, "It's getting worse"),
    EVEN(null, "prog-gleich", null, "Remained the same"),
    UNKNOWN("question", null, null, "Unknown"),
    NO_DATA(null, null, "—", "No data available");

    private final String emoticon;
    private final String hipchatEmoticon;
    private final String unicode;
    private final String description;

    Trend(String emoticon, String hipchatEmoticon, String unicode, String description) {
        this.emoticon = emoticon;
        this.hipchatEmoticon = hipchatEmoticon;
        this.unicode = unicode;
        this.description = description;
    }

}
