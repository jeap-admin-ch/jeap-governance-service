package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.confluence.style.ColorUtility;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingScore;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class SystemScoreReportModel {
    private String systemName;
    private String pageSuffix;
    private int score;
    private Trend trend;
    private List<ReportingScore> scores;
    private List<ComponentScoreReportModel> componentScores;

    public String getHighlightColor() {
        return ColorUtility.getHighlightColor(score);
    }
}
