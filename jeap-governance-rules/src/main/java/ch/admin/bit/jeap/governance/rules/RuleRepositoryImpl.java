package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluation;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.rules.RuleConfigurationProperties.ActiveRule;
import ch.admin.bit.jeap.governance.rules.RuleConfigurationProperties.ComponentExemption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

@Component
@Slf4j
class RuleRepositoryImpl implements RuleRepository {

    private final Map<RuleId, Rule> rulesByIdMap;
    /// `RuleConfigurationProperties` is in refresh scope, thus injected directly here to always have the latest configuration available when evaluating rules
    private final RuleConfigurationProperties properties;

    RuleRepositoryImpl(List<Rule> rules, RuleConfigurationProperties properties) {
        this.rulesByIdMap = rules.stream()
                .collect(Collectors.toMap(rule -> rule.metadata().ruleId(), identity()));
        this.properties = properties;
        String activeRules = properties.getActive().stream()
                .map(a -> a.getId().toString()).sorted().collect(joining(","));
        log.info("RuleRepository initialized with {} rule(s) and {} active rule(s) configured. Active rules: {}",
                rulesByIdMap.size(), properties.getActive().size(), activeRules);
    }

    @Override
    public List<RuleEvaluation> getRulesToEvaluateForComponent(SystemComponent systemComponent) {
        return properties.getActive().stream()
                .filter(activeRule -> rulesByIdMap.containsKey(activeRule.getId()))
                .map(activeRule -> toRuleEvaluation(activeRule, systemComponent))
                .toList();
    }

    @Override
    public List<RuleId> getActiveRuleIds() {
        return properties.getActive().stream()
                .map(ActiveRule::getId)
                .filter(rulesByIdMap::containsKey)
                .toList();
    }

    @Override
    public List<RuleInfo> getActiveRuleInfos() {
        return properties.getActive().stream()
                .map(activeRule -> {
                    Rule rule = rulesByIdMap.get(activeRule.getId());
                    if (rule == null) {
                        return null;
                    }
                    RuleMetadata metadata = rule.metadata();
                    return new RuleInfo(metadata.ruleId(), metadata.label(), activeRule.getDocumentationLink());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Map<RuleId, Integer> getActiveRuleWeights() {
        return properties.getActive().stream().collect(Collectors.toMap(
                ActiveRule::getId,
                ActiveRule::getWeight));
    }

    private RuleEvaluation toRuleEvaluation(ActiveRule activeRule, SystemComponent component) {
        var rule = rulesByIdMap.get(activeRule.getId());
        var optionalExemption = findExemption(rule.metadata().ruleId(), component);
        var activationState = determineActivationState(optionalExemption);
        var parameters = getRuleParameters(activeRule, optionalExemption);
        return new RuleEvaluation(rule, parameters, activationState);
    }

    private static RuleParameters getRuleParameters(ActiveRule activeRule, Optional<ComponentExemption> optionalExemption) {
        var exemptionParameters = optionalExemption.map(ComponentExemption::getParameters).orElse(Map.of());
        return RuleParameters.of(activeRule.getParameters(), exemptionParameters);
    }

    private Optional<ComponentExemption> findExemption(RuleId ruleId, SystemComponent component) {
        return properties.getComponentExemptions().stream()
                .filter(exemption -> exemption.getComponentName().equals(component.getName()))
                .filter(exemption -> exemption.getRuleIds().contains(ruleId))
                .findFirst();
    }

    private static RuleActivationState determineActivationState(Optional<ComponentExemption> optionalExemption) {
        return optionalExemption
                .map(exemption -> RuleActivationState.stateForRuleWithOptionalExemption(LocalDate.now(), exemption.getUntil()))
                .orElse(RuleActivationState.ACTIVE);
    }

}
