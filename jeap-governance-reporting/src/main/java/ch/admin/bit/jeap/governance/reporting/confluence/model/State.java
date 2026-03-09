package ch.admin.bit.jeap.governance.reporting.confluence.model;

import lombok.Getter;

/**
 * To determine which emoticon, emoji, etc. to use, test it in Confluence.
 *
 * For example, if your pageId is {@code 1390357996}, you can retrieve the
 * page content via:
 * {@code https://yourconfluence.xy/rest/api/content/1390357996?expand=body.storage}
 */
@Getter
public enum State {
    OK("tick", null, null, null, "Compliant"),
    FAIL("cross", null, null, null, "Non-compliant"),
    PAUSED("pause button", null, null, "23f8", "Paused"),
    DISABLED("cross mark button", null, null, "274e", "Disabled"),
    UNKNOWN("question", null, null, null, "Unknown");

    private final String emoticon;
    private final String hipchatEmoticon;
    private final String unicode;
    private final String emojiId;
    private final String description;

    State(String emoticon, String hipchatEmoticon, String unicode, String emojiId, String description) {
        this.emoticon = emoticon;
        this.hipchatEmoticon = hipchatEmoticon;
        this.unicode = unicode;
        this.emojiId = emojiId;
        this.description = description;
    }
}
