package ch.admin.bit.jeap.governance.rules.dependency.webconfig;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(PromTimeSeriesQueryRepository.class)
@RequiredArgsConstructor
public class ComponentUsesWebConfigStarterRule implements Rule {

    private static final String WEB_CONFIG_STARTER_ARTIFACT_ID = "jeap-web-config-starter";

    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @Override
    public RuleMetadata metadata() {
        return RuleMetadata.builder()
                .ruleId(RuleId.of("component-uses-web-config-starter"))
                .label("Component Uses jEAP Web Config Starter")
                .build();
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        if (systemComponent.getType() != ComponentType.SELF_CONTAINED_SYSTEM) {
            return RuleResult.ok("Not applicable (not a self-contained system)");
        }

        boolean usesStarter = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, systemComponent.getId()).stream()
                .map(promTimeSeries -> promTimeSeries.getSample().metric().get("name"))
                .filter(Objects::nonNull)
                .anyMatch(this::isWebConfigStarter);

        return usesStarter
                ? RuleResult.ok(WEB_CONFIG_STARTER_ARTIFACT_ID + " is used")
                : RuleResult.failed("Self-contained system does not use " + WEB_CONFIG_STARTER_ARTIFACT_ID);
    }

    /**
     * Matches the dependency name against the web config starter artifact id. The dependency name reported by Prometheus
     * is usually a {@code groupId:artifactId} coordinate, but may also be a bare artifact id, so we compare the artifact
     * id part (the segment after the last colon).
     */
    private boolean isWebConfigStarter(String dependencyName) {
        String artifactId = dependencyName.contains(":")
                ? dependencyName.substring(dependencyName.lastIndexOf(':') + 1)
                : dependencyName;
        return WEB_CONFIG_STARTER_ARTIFACT_ID.equals(artifactId);
    }
}
