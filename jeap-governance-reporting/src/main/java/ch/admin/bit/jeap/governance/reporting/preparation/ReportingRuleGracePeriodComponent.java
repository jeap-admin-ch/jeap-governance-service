package ch.admin.bit.jeap.governance.reporting.preparation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportingRuleGracePeriodComponent {
    private final Long systemId;
    private final String systemName;
    private final Long componentId;
    private final String componentName;
    private final String stateComment;
    private final ZonedDateTime violationDetectedAt;
    private final ZonedDateTime gracePeriodEndsAt;
}
