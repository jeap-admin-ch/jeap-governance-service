package ch.admin.bit.jeap.governance.reporting.confluence.style;

import ch.admin.bit.jeap.governance.reporting.confluence.model.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ColorUtilityTest {

    @Test
    void getHighlightColorScore() {
        assertEquals("#79f2c0", ColorUtility.getHighlightColor(80));
        assertEquals("#79f2c0", ColorUtility.getHighlightColor(100));
        assertEquals("#fff0b3", ColorUtility.getHighlightColor(50));
        assertEquals("#fff0b3", ColorUtility.getHighlightColor(79));
        assertEquals("#ffbdad", ColorUtility.getHighlightColor(0));
        assertEquals("#ffbdad", ColorUtility.getHighlightColor(49));
        assertNull(ColorUtility.getHighlightColor(-1));
    }

    @Test
    void getHighlightColorState() {
        assertEquals("#79f2c0", ColorUtility.getHighlightColor(State.OK));
        assertEquals("#ffbdad", ColorUtility.getHighlightColor(State.FAIL));
        assertNull(ColorUtility.getHighlightColor(State.UNKNOWN));
        assertNull(ColorUtility.getHighlightColor(State.PAUSED));
        assertNull(ColorUtility.getHighlightColor(State.DISABLED));
    }
}
