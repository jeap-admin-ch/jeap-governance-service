package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.confluence.style.BackgroundUtility;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RuleReportModel {
    private String ruleId;
    private String name;
    private String documentationLink;
    private int conformanceRate;
    private Trend trend;
    private List<RuleReportSystemModel> systems;
    private List<RuleReportComponentModel> nonCompliantComponents;

    public String getBackgroundColor() {
        return BackgroundUtility.getBackgroundColor(conformanceRate);
    }
}
