package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.confluence.style.ColorUtility;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

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

    public String getStateCommentHeading() {
        return stateComment == null ? null : stateComment.lines().findFirst().orElse("");
    }

    public List<String> getStateCommentDetails() {
        return stateComment == null ? List.of() : stateComment.lines().skip(1)
                .filter(line -> !line.isBlank())
                .toList();
    }

    public boolean hasStateCommentDetails() {
        return !getStateCommentDetails().isEmpty();
    }
}
