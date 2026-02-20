package ch.admin.bit.jeap.governance.reporting.confluence.style;

import ch.admin.bit.jeap.governance.reporting.confluence.model.State;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BackgroundUtility {

    public static String getBackgroundColor(int value) {
        if (value >= 80) {
            return "background-color: #e3fcef;";
        }
        if (value >= 50) {
            return "background-color: #fff7e6;";
        }
        if (value >= 0) {
            return "background-color: #ffe9e9;";
        }
        return "background-color: #f0f0f0;";
    }

    public static String getBackgroundColor(State state) {
        return switch (state) {
            case OK -> "background-color: #e3fcef;";
            case FAIL -> "background-color: #ffe9e9;";
            default -> "background-color: #f0f0f0;";
        };
    }

}
