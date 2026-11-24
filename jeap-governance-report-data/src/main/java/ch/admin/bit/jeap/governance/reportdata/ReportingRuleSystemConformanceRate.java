package ch.admin.bit.jeap.governance.reportdata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ReportingRuleSystemConformanceRate {
    @Getter
    private final String systemName;
    @Getter
    private final int latestConformanceRate;

}
