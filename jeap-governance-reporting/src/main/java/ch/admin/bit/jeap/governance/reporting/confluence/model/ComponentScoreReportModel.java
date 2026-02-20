package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.confluence.style.BackgroundUtility;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingScore;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ComponentScoreReportModel {
    private String componentName;
    private String pageSuffix;
    private int score;
    private Trend trend;
    private List<ReportingScore> scores;
    private List<RuleStateReportModel> ruleStates;

    public String getBackgroundColor() {
        return BackgroundUtility.getBackgroundColor(score);
    }
}
