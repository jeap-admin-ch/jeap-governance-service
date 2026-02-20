package ch.admin.bit.jeap.governance.reporting.confluence.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Builder
@Data
public class RuleReportComponentModel {
    private long id;
    private String name;
    private String pageSuffix;
    private String systemName;
    private String systemPageSuffix;
    private ZonedDateTime nonCompliantSince;
}
