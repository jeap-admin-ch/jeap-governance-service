package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static ch.admin.bit.jeap.governance.reporting.preparation.TrendIndicatorUtility.calculateTrendIndicator;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportingRule {
    @Getter
    private final String ruleId;
    @Getter
    private final String ruleName;
    @Getter
    private final String documentationLink;

    private final List<ReportingRuleConformanceRate> conformanceRates = new ArrayList<>();
    private final List<ReportingRuleSystemConformanceRate> systemConformanceRates = new ArrayList<>();
    private final List<ReportingRuleNonCompliantComponent> nonCompliantComponents = new ArrayList<>();

    void addConformanceRate(RuleConformanceRate conformanceRate) {
        conformanceRates.add(new ReportingRuleConformanceRate(conformanceRate));
        Collections.sort(conformanceRates);
    }

    void addSystemConformanceRate(long systemId, String systemName, int latestConformanceRate) {
        systemConformanceRates.add(new ReportingRuleSystemConformanceRate(systemId, systemName, latestConformanceRate));
        systemConformanceRates.sort(Comparator.comparing(ReportingRuleSystemConformanceRate::getSystemName));
    }

    void addNonCompliantComponents(long systemId, String systemName, long componentId, String componentName, ZonedDateTime nonComplianceSince) {
        nonCompliantComponents.add(new ReportingRuleNonCompliantComponent(systemId, systemName, componentId, componentName, nonComplianceSince));
        // sort by systemName, then componentName
        nonCompliantComponents.sort(
                Comparator.comparing(ReportingRuleNonCompliantComponent::getSystemName)
                        .thenComparing(ReportingRuleNonCompliantComponent::getComponentName)
        );
    }

    public TrendIndicator getConformanceRateTrend() {
        return calculateTrendIndicator(conformanceRates);
    }

    public int getLatestConformanceRate() {
        return conformanceRates.isEmpty() ? 0 : conformanceRates.getLast().getRate();
    }

    public List<ReportingRuleConformanceRate> getConformanceRates() {
        return new ArrayList<>(conformanceRates);
    }

    public List<ReportingRuleSystemConformanceRate> getSystemConformanceRates() {
        return new ArrayList<>(systemConformanceRates);
    }

    public List<ReportingRuleNonCompliantComponent> getNonCompliantComponents() {
        return new ArrayList<>(nonCompliantComponents);
    }
}
