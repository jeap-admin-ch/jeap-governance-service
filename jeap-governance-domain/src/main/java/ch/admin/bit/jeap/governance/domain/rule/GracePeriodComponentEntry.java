package ch.admin.bit.jeap.governance.domain.rule;

import java.time.ZonedDateTime;

public interface GracePeriodComponentEntry {
    Long getSystemId();
    Long getSystemComponentId();
    String getRuleId();
    ZonedDateTime getViolationDetectedAt();
}
