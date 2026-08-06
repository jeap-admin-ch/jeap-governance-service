package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.*;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.*;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.ComponentScoreRepository;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static ch.admin.bit.jeap.governance.domain.ComponentType.isIgnoredForGovernance;
import static java.util.stream.Collectors.toUnmodifiableSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingDataAccess {

    private final SystemScoreRepository systemScoreRepository;
    private final ComponentScoreRepository componentScoreRepository;
    private final RuleRepository ruleRepository;
    private final RuleStateRepository ruleStateRepository;
    private final RuleConformanceRateRepository ruleConformanceRateRepository;
    private final SystemRuleConformanceRateRepository systemRuleConformanceRateRepository;
    private final SystemRepository systemRepository;
    private final SystemComponentRepository systemComponentRepository;

    List<SystemScore> findAllSystemScoresByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay) {
        return systemScoreRepository.findAllByDayBetweenInclusive(fromDay, toDay);
    }

    List<ComponentScore> findAllComponentScoreByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay) {
        return componentScoreRepository.findAllByDayBetweenInclusive(fromDay, toDay);
    }

    List<RuleInfo> findAllActiveRuleInfos() {
        return ruleRepository.getActiveRuleInfos();
    }

    List<RuleState> findAllRuleStates() {
        return ruleStateRepository.findAll();
    }

    List<NonCompliantComponentEntry> findNonCompliantSince() {
        return ruleStateRepository.findNonCompliantSince();
    }

    List<RuleConformanceRate> findAllRuleConformanceByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay) {
        return ruleConformanceRateRepository.findAllByDayBetweenInclusive(fromDay, toDay);
    }

    List<SystemRuleConformanceRate> findLatestPerRuleIdAndSystemId() {
        return systemRuleConformanceRateRepository.findLatestPerRuleIdAndSystemId();
    }

    public List<SystemReference> findAllSystemReferences() {
        return systemRepository.findAllSystemReferences();
    }

    public List<SystemComponentReference> findAllComponentReferences() {
        return systemComponentRepository.findAllSystemComponentReferences();
    }

    Set<Long> findIgnoredComponentIds() {
        return systemRepository.findAll().stream()
                .flatMap(system -> system.getSystemComponents().stream())
                .filter(component -> isIgnoredForGovernance(component.getType()))
                .map(SystemComponent::getId)
                .filter(Objects::nonNull)
                .collect(toUnmodifiableSet());
    }

}
