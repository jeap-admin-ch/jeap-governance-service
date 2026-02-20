package ch.admin.bit.jeap.governance.domain.rule;

import java.time.ZonedDateTime;

public interface NonCompliantComponentEntry {
    Long getSystemId();
    Long getSystemComponentId();
    String getSystemComponentName();
    String getRuleId();
    String getStateComment();
    ZonedDateTime getNonCompliantSince();
}
