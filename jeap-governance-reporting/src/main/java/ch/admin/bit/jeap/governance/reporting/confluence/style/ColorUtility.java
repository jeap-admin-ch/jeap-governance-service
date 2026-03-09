package ch.admin.bit.jeap.governance.reporting.confluence.style;

import ch.admin.bit.jeap.governance.reporting.confluence.model.State;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ColorUtility {

    private static final String GREEN = "#79f2c0";
    private static final String YELLOW = "#fff0b3";
    private static final String RED = "#ffbdad";

    public static String getHighlightColor(int value) {
        if (value >= 80) {
            return GREEN;
        }
        if (value >= 50) {
            return YELLOW;
        }
        if (value >= 0) {
            return RED;
        }
        return null;
    }

    public static String getHighlightColor(State state) {
        return switch (state) {
            case OK -> GREEN;
            case FAIL -> RED;
            default -> null;
        };
    }
}
