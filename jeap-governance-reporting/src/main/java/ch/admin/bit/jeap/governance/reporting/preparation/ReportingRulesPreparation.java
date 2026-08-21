package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import ch.admin.bit.jeap.governance.domain.SystemReference;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.GracePeriodComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.NonCompliantComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ReportingRulesPreparation {

    private final ReportingDataAccess dataAccess;
    private final Clock clock;

    @Autowired
    public ReportingRulesPreparation(ReportingDataAccess dataAccess) {
        this(dataAccess, Clock.systemDefaultZone());
    }

    ReportingRulesPreparation(ReportingDataAccess dataAccess, Clock clock) {
        this.dataAccess = dataAccess;
        this.clock = clock;
    }

    @TransactionalReadReplica
    @Timed("jeap.governance.service.reporting.rules.preparation")
    public List<ReportingRule> prepareAllRules(LocalDate fromDay, LocalDate toDay) {
        List<RuleConformanceRate> latestRuleConformanceRatesPerRuleId = dataAccess.findAllRuleConformanceByDayBetweenInclusive(fromDay, toDay);
        List<SystemRuleConformanceRate> latestPerRuleIdAndSystemId = dataAccess.findLatestPerRuleIdAndSystemId();
        List<RuleInfo> activeRuleInfos = dataAccess.findAllActiveRuleInfos();
        List<NonCompliantComponentEntry> nonCompliantComponentEntries = dataAccess.findNonCompliantSince();
        List<GracePeriodComponentEntry> gracePeriodComponentEntries = dataAccess.findGracePeriodComponents();
        List<SystemReference> allSystemReferences = dataAccess.findAllSystemReferences();
        List<SystemComponentReference> allComponentReferences = dataAccess.findAllComponentReferences();
        Set<Long> ignoredComponentIds = dataAccess.findIgnoredComponentIds();

        return prepareRules(latestRuleConformanceRatesPerRuleId, latestPerRuleIdAndSystemId, activeRuleInfos,
                nonCompliantComponentEntries, gracePeriodComponentEntries, allSystemReferences, allComponentReferences,
                ignoredComponentIds);
    }

    List<ReportingRule> prepareRules(List<RuleConformanceRate> latestRuleConformanceRatesPerRuleId, List<SystemRuleConformanceRate> latestPerRuleIdAndSystemId, List<RuleInfo> activeRuleInfos,
                                    List<NonCompliantComponentEntry> nonCompliantComponentEntries,
                                    List<GracePeriodComponentEntry> gracePeriodComponentEntries,
                                    List<SystemReference> allSystemReferences,
                                    List<SystemComponentReference> allComponentReferences, Set<Long> ignoredComponentIds) {
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
        Map<String, List<GracePeriodComponentEntry>> gracePeriodComponentEntryByRuleId = gracePeriodComponentEntries.stream()
                .collect(Collectors.groupingBy(GracePeriodComponentEntry::getRuleId));

        return buildRules(activeRuleInfos, ruleConformanceRateByRuleId, systemRuleConformanceRateByRuleIdAndSystemId,
                systemReferenceById, nonCompliantComponentEntryByRuleId, gracePeriodComponentEntryByRuleId,
                componentReferenceById, ignoredComponentIds);
    }

    private List<ReportingRule> buildRules(List<RuleInfo> activeRuleInfos, Map<String, List<RuleConformanceRate>> ruleConformanceRateByRuleId,
                                           Map<String, List<SystemRuleConformanceRate>> systemRuleConformanceRateByRuleIdAndSystemId,
                                           Map<Long, SystemReference> systemReferenceById,
                                           Map<String, List<NonCompliantComponentEntry>> nonCompliantComponentEntryByRuleId,
                                           Map<String, List<GracePeriodComponentEntry>> gracePeriodComponentEntryByRuleId,
                                           Map<Long, SystemComponentReference> componentReferenceById,
                                           Set<Long> ignoredComponentIds) {
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
                     ruleInfo.documentationLink(),
                     ruleInfo.violationDelay()
             );
            ruleConformanceRates.forEach(reportingRule::addConformanceRate);
            addSystemRuleConformanceRates(reportingRule, systemRuleConformanceRateByRuleIdAndSystemId, systemReferenceById);
            addNonCompliantComponents(reportingRule, nonCompliantComponentEntryByRuleId, systemReferenceById,
                    componentReferenceById, ignoredComponentIds);
            addGracePeriodComponents(reportingRule, ruleInfo, gracePeriodComponentEntryByRuleId, systemReferenceById,
                    componentReferenceById, ignoredComponentIds);

            result.add(reportingRule);
        }
        return result;
    }

    private void addGracePeriodComponents(ReportingRule reportingRule, RuleInfo ruleInfo,
                                          Map<String, List<GracePeriodComponentEntry>> entriesByRuleId,
                                          Map<Long, SystemReference> systemReferenceById,
                                          Map<Long, SystemComponentReference> componentReferenceById,
                                          Set<Long> ignoredComponentIds) {
        if (!reportingRule.hasViolationGracePeriod()) {
            return;
        }
        for (GracePeriodComponentEntry entry : entriesByRuleId.getOrDefault(reportingRule.getRuleId(), List.of())) {
            if (ignoredComponentIds.contains(entry.getSystemComponentId())) {
                continue;
            }
            SystemReference systemReference = systemReferenceById.get(entry.getSystemId());
            SystemComponentReference componentReference = componentReferenceById.get(entry.getSystemComponentId());
            if (systemReference == null || componentReference == null) {
                log.warn("Could not resolve system or component reference for grace period entry: {}", entry);
                continue;
            }
            ZonedDateTime violationDetectedAt = entry.getViolationDetectedAt();
            ZonedDateTime gracePeriodEndsAt = violationDetectedAt.plus(ruleInfo.violationDelay());
            if (!gracePeriodEndsAt.isAfter(ZonedDateTime.now(clock))) {
                continue;
            }
            reportingRule.addGracePeriodComponent(
                    systemReference.getId(),
                    systemReference.getName(),
                    componentReference.getId(),
                    componentReference.getName(),
                    entry.getStateComment(),
                    violationDetectedAt,
                    gracePeriodEndsAt);
        }
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

    private static void addNonCompliantComponents(ReportingRule reportingRule,
                                                  Map<String, List<NonCompliantComponentEntry>> nonCompliantComponentEntryByRuleId,
                                                  Map<Long, SystemReference> systemReferenceById,
                                                  Map<Long, SystemComponentReference> componentReferenceById,
                                                  Set<Long> ignoredComponentIds) {
        List<NonCompliantComponentEntry> nonCompliantComponentEntriesForRule = nonCompliantComponentEntryByRuleId.get(reportingRule.getRuleId());
        if (nonCompliantComponentEntriesForRule == null || nonCompliantComponentEntriesForRule.isEmpty()) {
            log.info("No non compliant component entries found for rule id {}, rule label {}", reportingRule.getRuleId(), reportingRule.getRuleName());
        } else {
            for (NonCompliantComponentEntry nonCompliantComponentEntry : nonCompliantComponentEntriesForRule) {
                addNonCompliantComponentEntry(nonCompliantComponentEntry, reportingRule, systemReferenceById,
                        componentReferenceById, ignoredComponentIds);
            }
        }
    }

    private static void addNonCompliantComponentEntry(NonCompliantComponentEntry nonCompliantComponentEntry,
                                                      ReportingRule reportingRule,
                                                      Map<Long, SystemReference> systemReferenceById,
                                                      Map<Long, SystemComponentReference> componentReferenceById,
                                                      Set<Long> ignoredComponentIds) {
        if (ignoredComponentIds.contains(nonCompliantComponentEntry.getSystemComponentId())) {
            return;
        }
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
