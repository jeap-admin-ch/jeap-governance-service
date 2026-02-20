package ch.admin.bit.jeap.governance.reporting.preparation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;

@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ReportingRuleNonCompliantComponent {
    @Getter
    private final Long systemId;
    @Getter
    private final String systemName;
    @Getter
    private final Long componentId;
    @Getter
    private final String componentName;
    @Getter
    private final ZonedDateTime nonComplianceSince;

}
