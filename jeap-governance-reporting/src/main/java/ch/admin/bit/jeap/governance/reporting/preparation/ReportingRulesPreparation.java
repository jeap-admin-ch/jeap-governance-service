package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import ch.admin.bit.jeap.governance.domain.SystemReference;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.NonCompliantComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingRulesPreparation {

    private final ReportingDataAccess dataAccess;

    @TransactionalReadReplica
    @Timed("jeap.governance.service.reporting.rules.prepararation")
    public List<ReportingRule> prepareAllRules(LocalDate fromDay, LocalDate toDay) {
        List<RuleConformanceRate> latestRuleConformanceRatesPerRuleId = dataAccess.findAllRuleConformanceByDayBetweenInclusive(fromDay, toDay);
        List<SystemRuleConformanceRate> latestPerRuleIdAndSystemId = dataAccess.findLatestPerRuleIdAndSystemId();
        List<RuleInfo> activeRuleInfos = dataAccess.findAllActiveRuleInfos();
        List<NonCompliantComponentEntry> nonCompliantComponentEntries = dataAccess.findNonCompliantSince();
        List<SystemReference> allSystemReferences = dataAccess.findAllSystemReferences();
        List<SystemComponentReference> allComponentReferences = dataAccess.findAllComponentReferences();

        return prepareAllRules(latestRuleConformanceRatesPerRuleId, latestPerRuleIdAndSystemId, activeRuleInfos, nonCompliantComponentEntries, allSystemReferences, allComponentReferences);
    }

    List<ReportingRule> prepareAllRules(List<RuleConformanceRate> latestRuleConformanceRatesPerRuleId, List<SystemRuleConformanceRate> latestPerRuleIdAndSystemId, List<RuleInfo> activeRuleInfos,
                                        List<NonCompliantComponentEntry> nonCompliantComponentEntries, List<SystemReference> allSystemReferences, List<SystemComponentReference> allComponentReferences) {
        Map<Long, SystemReference> systemReferenceById = allSystemReferences.stream()
                .collect(Collectors.toMap(SystemReference::getId, systemReference -> systemReference));
        Map<Long, SystemComponentReference> componentReferenceById = allComponentReferences.stream()
                .collect(Collectors.toMap(SystemComponentReference::getId, componentReference -> componentReference));
        Map<String, List<RuleConformanceRate>> ruleConformanceRateByRuleId = latestRuleConformanceRatesPerRuleId.stream()
                .collect(Collectors.groupingBy(RuleConformanceRate::getRuleId));
        Map<String, List<SystemRuleConformanceRate>> systemRuleConformanceRateByRuleIdAndSystemId = latestPerRuleIdAndSystemId.stream()
                .collect(Collectors.groupingBy(SystemRuleConformanceRate::getRuleId));
        Map<String, List<NonCompliantComponentEntry>> nonCompliantComponentEntryByRuleId = nonCompliantComponentEntries.stream()
                .collect(Collectors.groupingBy(NonCompliantComponentEntry::getRuleId));

        return prepareAllRules(activeRuleInfos, ruleConformanceRateByRuleId, systemRuleConformanceRateByRuleIdAndSystemId, systemReferenceById, nonCompliantComponentEntryByRuleId, componentReferenceById);
    }

    private List<ReportingRule> prepareAllRules(List<RuleInfo> activeRuleInfos, Map<String, List<RuleConformanceRate>> ruleConformanceRateByRuleId, Map<String, List<SystemRuleConformanceRate>> systemRuleConformanceRateByRuleIdAndSystemId, Map<Long, SystemReference> systemReferenceById, Map<String, List<NonCompliantComponentEntry>> nonCompliantComponentEntryByRuleId, Map<Long, SystemComponentReference> componentReferenceById) {
        List<ReportingRule> result = new ArrayList<>();
        for (RuleInfo ruleInfo : activeRuleInfos) {
            String ruleId = ruleInfo.ruleId().id();
            List<RuleConformanceRate> ruleConformanceRates = ruleConformanceRateByRuleId.get(ruleId);
            if (ruleConformanceRates == null) {
                log.warn("No RuleConformanceRates found for rule id {}, rule label {}", ruleId, ruleInfo.label());
                continue;
            }
            ReportingRule reportingRule = new ReportingRule(
                    ruleId,
                    ruleInfo.label(),
                    ruleInfo.documentationLink()
            );
            ruleConformanceRates.forEach(reportingRule::addConformanceRate);
            addSystemRuleConformanceRates(reportingRule, systemRuleConformanceRateByRuleIdAndSystemId, systemReferenceById);
            addNonCompliantComponents(reportingRule, nonCompliantComponentEntryByRuleId, systemReferenceById, componentReferenceById);

            result.add(reportingRule);
        }
        return result;
    }

    private static void addSystemRuleConformanceRates(ReportingRule reportingRule, Map<String, List<SystemRuleConformanceRate>> systemRuleConformanceRateByRuleIdAndSystemId, Map<Long, SystemReference> systemReferenceById) {
        List<SystemRuleConformanceRate> systemRuleConformanceRateBySystemId = systemRuleConformanceRateByRuleIdAndSystemId.get(reportingRule.getRuleId());
        if (systemRuleConformanceRateBySystemId == null || systemRuleConformanceRateBySystemId.isEmpty()) {
            log.warn("No SystemRuleConformanceRates found for rule id {}, rule label {}", reportingRule.getRuleId(), reportingRule.getRuleName());
            return;
        }
        for (SystemRuleConformanceRate systemRuleConformanceRate : systemRuleConformanceRateBySystemId) {
            SystemReference systemReference = systemReferenceById.get(systemRuleConformanceRate.getSystemId());
            if (systemReference == null) {
                log.warn("Could not find any system reference for system id: {}, system rule conformance rate: {}", systemRuleConformanceRate.getSystemId(), systemRuleConformanceRate);
            } else {
                reportingRule.addSystemConformanceRate(systemRuleConformanceRate.getSystemId(), systemReference.getName(), systemRuleConformanceRate.getConformanceRate());
            }

        }
    }

    private static void addNonCompliantComponents(ReportingRule reportingRule, Map<String, List<NonCompliantComponentEntry>> nonCompliantComponentEntryByRuleId, Map<Long, SystemReference> systemReferenceById, Map<Long, SystemComponentReference> componentReferenceById) {
        List<NonCompliantComponentEntry> nonCompliantComponentEntriesForRule = nonCompliantComponentEntryByRuleId.get(reportingRule.getRuleId());
        if (nonCompliantComponentEntriesForRule == null || nonCompliantComponentEntriesForRule.isEmpty()) {
            log.info("No non compliant component entries found for rule id {}, rule label {}", reportingRule.getRuleId(), reportingRule.getRuleName());
        } else {
            for (NonCompliantComponentEntry nonCompliantComponentEntry : nonCompliantComponentEntriesForRule) {
                addNonCompliantComponentEntry(nonCompliantComponentEntry, reportingRule, systemReferenceById, componentReferenceById);
            }
        }
    }

    private static void addNonCompliantComponentEntry(NonCompliantComponentEntry nonCompliantComponentEntry, ReportingRule reportingRule, Map<Long, SystemReference> systemReferenceById, Map<Long, SystemComponentReference> componentReferenceById) {
        SystemReference systemReference = systemReferenceById.get(nonCompliantComponentEntry.getSystemId());
        if (systemReference == null) {
            log.warn(
                    "Could not find any system reference for system id: {}, non compliant component entry: {}",
                    nonCompliantComponentEntry.getSystemId(),
                    nonCompliantComponentEntry);
            return;
        }
        SystemComponentReference componentReference =
                componentReferenceById.get(nonCompliantComponentEntry.getSystemComponentId());

        if (componentReference == null) {
            log.warn(
                    "Could not find any component reference for component id: {}, non compliant component entry: {}",
                    nonCompliantComponentEntry.getSystemComponentId(),
                    nonCompliantComponentEntry);
            return;
        }
        reportingRule.addNonCompliantComponents(
                systemReference.getId(),
                systemReference.getName(),
                componentReference.getId(),
                componentReference.getName(),
                nonCompliantComponentEntry.getNonCompliantSince()
        );
    }
}
