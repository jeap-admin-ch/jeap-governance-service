package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleExplanation;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleComponentExemption;
import ch.admin.bit.jeap.governance.domain.rule.RuleDescription;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluation;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.rules.RuleConfigurationProperties.ActiveRule;
import ch.admin.bit.jeap.governance.rules.RuleConfigurationProperties.ComponentExemption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
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
    private final Map<RuleId, String> explanationsByRuleId;

    RuleRepositoryImpl(List<Rule> rules, RuleConfigurationProperties properties) {
        this.rulesByIdMap = rules.stream()
                .collect(Collectors.toMap(rule -> rule.metadata().ruleId(), identity()));
        this.properties = properties;
        this.explanationsByRuleId = loadExplanations(rules);
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
    @SuppressWarnings("deprecation") // the documentation link is still delivered to the Confluence reporting
    public List<RuleInfo> getActiveRuleInfos() {
        return properties.getActive().stream()
                .map(activeRule -> {
                    Rule rule = rulesByIdMap.get(activeRule.getId());
                    if (rule == null) {
                        return null;
                    }
                    RuleMetadata metadata = rule.metadata();
                    return new RuleInfo(metadata.ruleId(), metadata.label(), activeRule.getDocumentationLink(),
                            activeRule.getViolationDelay());
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

    @Override
    public Optional<Map<String, String>> getActiveRuleParameters(RuleId ruleId) {
        return properties.getActive().stream()
                .filter(activeRule -> activeRule.getId().equals(ruleId))
                .filter(activeRule -> rulesByIdMap.containsKey(activeRule.getId()))
                .findFirst()
                .map(ActiveRule::getParameters);
    }

    @Override
    public List<RuleDescription> getKnownRules() {
        return rulesByIdMap.values().stream()
                .map(Rule::metadata)
                .sorted(Comparator.comparing(metadata -> metadata.ruleId().id()))
                .map(this::describe)
                .toList();
    }

    private RuleDescription describe(RuleMetadata metadata) {
        RuleId ruleId = metadata.ruleId();
        String explanation = explanationsByRuleId.get(ruleId);
        return findActiveRule(ruleId)
                .map(activeRule -> new RuleDescription(ruleId, metadata.label(), explanation, true,
                        activeRule.getWeight(), activeRule.getViolationDelay(), Map.copyOf(activeRule.getParameters()),
                        exemptionsOf(ruleId)))
                .orElseGet(() -> new RuleDescription(ruleId, metadata.label(), explanation, false,
                        null, null, Map.of(), List.of()));
    }

    private Optional<ActiveRule> findActiveRule(RuleId ruleId) {
        return properties.getActive().stream()
                .filter(activeRule -> activeRule.getId().equals(ruleId))
                .findFirst();
    }

    private List<RuleComponentExemption> exemptionsOf(RuleId ruleId) {
        return properties.getComponentExemptions().stream()
                .filter(exemption -> exemption.getRuleIds().contains(ruleId))
                .map(exemption -> new RuleComponentExemption(exemption.getId(), exemption.getComponentName(),
                        exemption.getReason(), exemption.getUntil(), Map.copyOf(exemption.getParameters())))
                .toList();
    }

    private static Map<RuleId, String> loadExplanations(List<Rule> rules) {
        Map<RuleId, String> explanations = new HashMap<>();
        for (Rule rule : rules) {
            RuleId ruleId = rule.metadata().ruleId();
            RuleExplanation.load(ruleId, rule.getClass().getClassLoader()).ifPresentOrElse(
                    explanation -> explanations.put(ruleId, explanation),
                    () -> log.warn("Rule '{}' ships no explanation. Add it as the classpath resource '{}'.",
                            ruleId, RuleExplanation.resourcePath(ruleId)));
        }
        return explanations;
    }

    private RuleEvaluation toRuleEvaluation(ActiveRule activeRule, SystemComponent component) {
        var rule = rulesByIdMap.get(activeRule.getId());
        var optionalExemption = findExemption(rule.metadata().ruleId(), component);
        var activationState = determineActivationState(optionalExemption);
        var parameters = getRuleParameters(activeRule, optionalExemption);
        return new RuleEvaluation(rule, parameters, activationState, activeRule.getViolationDelay());
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
