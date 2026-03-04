package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.confluence.style.ColorUtility;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Builder
@Data
public class RuleStateReportModel {
    private String ruleId;
    private String label;
    private String documentationLink;
    private State state;
    private String stateComment;
    private ZonedDateTime modifiedAt;

    public String getHighlightColor() {
        return ColorUtility.getHighlightColor(state);
    }
}
