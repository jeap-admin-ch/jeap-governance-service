package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.confluence.style.BackgroundUtility;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RuleReportSystemModel {
    private long id;
    private String name;
    private int conformanceRate;
    private String pageSuffix;

    public String getBackgroundColor() {
        return BackgroundUtility.getBackgroundColor(conformanceRate);
    }
}
