package ch.admin.bit.jeap.governance.reactionobserver.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnBean({PromTimeSeriesQueryRepository.class, ReactionObserverComponentLastObservationDateRepository.class})
@RequiredArgsConstructor
public class ComponentObservesReactionsRule implements Rule {

    public static final String KEY_OBSERVATION_MAX_DELAY_IN_DAYS = "observation-max-delay-in-days";
    private static final String KEY_IGNORED_SERVICE_NAMES = "ignored-service-names";
    private static final int OBSERVATION_DELAY_DEFAULT_VALUE = 7;
    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    private final ReactionObserverComponentLastObservationDateRepository lastObservationDateRepository;

    @Override
    public RuleMetadata metadata() {
        return RuleMetadata.builder()
                .ruleId(RuleId.of("component-observes-reactions"))
                .label("Component Observes Reactions")
                .build();
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {

        if (!systemComponent.getType().equals(ComponentType.BACKEND_SERVICE) && !systemComponent.getType().equals(ComponentType.SELF_CONTAINED_SYSTEM)) {
            return RuleResult.ok("Not applicable");
        }

        if (ignoreComponent(ruleParameters, systemComponent.getName())) {
            return RuleResult.ok("Component ignored for this rule");
        }

        List<PromTimeSeries> messagingTotalQueryResponses = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, systemComponent.getId());
        if (messagingTotalQueryResponses.isEmpty()) {
            return RuleResult.ok("No messaging library detected");
        }

        Optional<ReactionObserverComponentLastObservationDate> lastObservationDate = lastObservationDateRepository.findByComponentId(systemComponent.getId());
        int observationDelay = getObservationDelay(ruleParameters);
        if (lastObservationDate.isEmpty()) {
            return RuleResult.failed("Component does not have a reaction graph and therefore does not observe reactions");
        } else if (lastObservationDate.get().getLastObservationDate().isBefore(LocalDate.now().minusDays(observationDelay))) {
            return RuleResult.failed("Component has not observed any reactions in the last " + observationDelay + " days. Last observation date: " + lastObservationDate.get().getLastObservationDate());
        } else {
            return RuleResult.ok("Component observes reactions. Last observation date: " + lastObservationDate.get().getLastObservationDate());
        }

    }

    private boolean ignoreComponent(RuleParameters ruleParameters, String serviceName) {
        if (!ruleParameters.parameters().isEmpty()) {
            for (String ignoredServiceName : ruleParameters.getParameterAsList(KEY_IGNORED_SERVICE_NAMES)) {
                if (serviceName.contains(ignoredServiceName.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getObservationDelay(RuleParameters ruleParameters) {
        if (!ruleParameters.parameters().isEmpty() && ruleParameters.parameters().containsKey(KEY_OBSERVATION_MAX_DELAY_IN_DAYS)) {
            return Integer.parseInt(ruleParameters.parameters().get(KEY_OBSERVATION_MAX_DELAY_IN_DAYS));
        }
        return OBSERVATION_DELAY_DEFAULT_VALUE;
    }

}
