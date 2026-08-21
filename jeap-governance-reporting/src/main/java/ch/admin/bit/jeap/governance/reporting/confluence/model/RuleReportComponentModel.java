package ch.admin.bit.jeap.governance.reporting.confluence.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
@Data
public class RuleReportComponentModel {
    private long id;
    private String name;
    private String pageSuffix;
    private String systemName;
    private String systemPageSuffix;
    private String stateComment;
    private ZonedDateTime nonCompliantSince;
    private ZonedDateTime violationDetectedAt;
    private ZonedDateTime gracePeriodEndsAt;

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
