package ch.admin.bit.jeap.governance.rules.core.metrics;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(PromTimeSeriesQueryRepository.class)
// This rule can only the activated if prometheus import is enabled
@RequiredArgsConstructor
public class ComponentProducesMetricsRule implements Rule {

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-produces-metrics"))
            .label("Component Produces Metrics")
            .build();

    private final PromTimeSeriesQueryRepository repository;

    @Override
    public RuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        if (!providesMetrics(systemComponent)) {
            return RuleResult.failed("No prometheus metrics found for component %s. Is the jeap-spring-boot-monitoring-starter added as a dependency and the monitoring configuration working?"
                    .formatted(systemComponent.getName()));
        }
        return RuleResult.ok();
    }

    private boolean providesMetrics(SystemComponent systemComponent) {
        return repository.anyTimeSeriesExistsBy(systemComponent.getId());
    }
}
