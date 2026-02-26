package ch.admin.bit.jeap.governance.secscan.filter;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluation;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpApiExemptions;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.SystemComponentHttpApi;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiIgnoreFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class ConfigurableSystemComponentHttpApiIgnoreFilter implements SystemComponentHttpApiIgnoreFilter {

    static final String RULE_ID = "endpoints-protected";

    private final SystemComponentRepository systemComponentRepository;
    private final RuleRepository ruleRepository;

    @Override
    public Result shouldIgnoreApi(SystemComponentHttpApi api) {
        Optional<RuleEvaluationData> ruleData = getRuleEvaluationData(api.systemComponentName());

        if (ruleData.isEmpty()) {
            return Result.notIgnored();
        }

        RuleActivationState activationState = ruleData.get().activationState();
        if (activationState != RuleActivationState.ACTIVE) {
            // When not active, return notIgnored for shouldIgnoreApi.
            // The shouldIgnoreEndpoint method will create the ignore-by-exemption results.
            return Result.notIgnored();
        }

        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(ruleData.get().parameters()));

        HttpApiExemptions.Result componentExemptionResult = exemptions.shouldExemptComponent(api.systemComponentName());
        if (componentExemptionResult.exempted()) {
            log.info("Ignoring API for component '{}' in environment '{}'. Reason: {}",
                    api.systemComponentName(), api.environment(), componentExemptionResult.reason());
            return Result.ignoredWithReason(componentExemptionResult.reason());
        }

        HttpApiExemptions.Result exemptionResult = exemptions.shouldExemptHttpApi(api);
        if (exemptionResult.exempted()) {
            log.info("Ignoring API for component '{}' in environment '{}'. Reason: {}",
                    api.systemComponentName(), api.environment(), exemptionResult.reason());
            return Result.ignoredWithReason(exemptionResult.reason());
        }

        return Result.notIgnored();
    }

    @Override
    public Result shouldIgnoreEndpoint(String systemComponentName, HttpEndpoint httpEndpoint, String environment) {
        Optional<RuleEvaluationData> ruleData = getRuleEvaluationData(systemComponentName);

        if (ruleData.isEmpty()) {
            return Result.notIgnored();
        }

        RuleActivationState activationState = ruleData.get().activationState();
        if (activationState != RuleActivationState.ACTIVE) {
            return Result.ignoredWithReason(
                    "Check of endpoints-protected rule exempted for the system component '" + systemComponentName + "'.");
        }

        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(ruleData.get().parameters()));

        HttpApiExemptions.Result componentExemptionResult = exemptions.shouldExemptComponent(systemComponentName);
        if (componentExemptionResult.exempted()) {
            log.info("Ignoring endpoint '{} {}' for component '{}'. Reason: {}",
                    httpEndpoint.method(), httpEndpoint.path(), systemComponentName, componentExemptionResult.reason());
            return Result.ignoredWithReason(componentExemptionResult.reason());
        }

        HttpApiExemptions.Result exemptionResult = exemptions.shouldExemptHttpEndpoint(httpEndpoint, environment);
        if (exemptionResult.exempted()) {
            log.info("Ignoring endpoint '{} {}' for component '{}'. Reason: {}",
                    httpEndpoint.method(), httpEndpoint.path(), systemComponentName, exemptionResult.reason());
            return Result.ignoredWithReason(exemptionResult.reason());
        }

        return Result.notIgnored();
    }

    private Optional<RuleEvaluationData> getRuleEvaluationData(String systemComponentName) {
        Optional<SystemComponent> systemComponent = systemComponentRepository.findByName(systemComponentName);
        if (systemComponent.isEmpty()) {
            log.debug("System component '{}' not found, not ignoring.", systemComponentName);
            return Optional.empty();
        }

        List<RuleEvaluation> ruleEvaluations = ruleRepository.getRulesToEvaluateForComponent(systemComponent.get());
        return ruleEvaluations.stream()
                .filter(re -> RuleId.of(RULE_ID).equals(re.rule().metadata().ruleId()))
                .findFirst()
                .map(re -> new RuleEvaluationData(re.activationState(), re.ruleParameters().parameters()));
    }

    private record RuleEvaluationData(RuleActivationState activationState, Map<String, String> parameters) {
    }
}
