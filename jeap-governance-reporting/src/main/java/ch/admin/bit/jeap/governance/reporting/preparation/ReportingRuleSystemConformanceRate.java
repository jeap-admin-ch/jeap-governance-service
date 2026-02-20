package ch.admin.bit.jeap.governance.reporting.preparation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ReportingRuleSystemConformanceRate {
    @Getter
    private final Long systemId;
    @Getter
    private final String systemName;
    @Getter
    private final int latestConformanceRate;

}
