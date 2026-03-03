package ch.admin.bit.jeap.governance.rules.core.dbschema;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnBean({PromTimeSeriesQueryRepository.class, DatabaseSchemaVersionRepository.class, DeploymentLogComponentVersionRepository.class})
@RequiredArgsConstructor
public class ComponentPublishesDbSchemaRule implements Rule {

    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;
    private final DatabaseSchemaVersionRepository databaseSchemaVersionRepository;
    private final DeploymentLogComponentVersionRepository deploymentLogComponentVersionRepository;

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-publishes-dbschema"))
            .label("Component Publishes DB Schema")
            .build();

    @Override
    public RuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        if (!systemComponent.getType().equals(ComponentType.BACKEND_SERVICE) && !systemComponent.getType().equals(ComponentType.SELF_CONTAINED_SYSTEM)) {
            return RuleResult.ok("Not applicable");
        }

        final String serviceName = systemComponent.getName();

        if (ignoreComponent(ruleParameters, serviceName)) {
            return RuleResult.ok("Component ignored for this rule");
        }

        List<PromTimeSeries> messagingTotalQueryResponses = promTimeSeriesQueryRepository.findBy(PromQueryType.JDBC_CONNECTIONS_ACTIVE, systemComponent.getId());

        if (messagingTotalQueryResponses.isEmpty()) {
            return RuleResult.ok("No database connections detected");
        }

        Optional<DatabaseSchemaVersion> databaseSchemaVersionOptional = databaseSchemaVersionRepository.findByComponentId(systemComponent.getId());

        if (databaseSchemaVersionOptional.isEmpty()) {
            return RuleResult.failed("No database schema found in the architecture repository, it has to be uploaded by the application");
        }

        DatabaseSchemaVersion databaseSchemaVersion = databaseSchemaVersionOptional.get();

        Optional<DeploymentLogComponentVersion> deploymentLogComponentVersionOptional = deploymentLogComponentVersionRepository.findByComponentId(systemComponent.getId());

        if (deploymentLogComponentVersionOptional.isPresent()) {
            String systemComponentVersionFromDeploymentLog = deploymentLogComponentVersionOptional.get().getVersion();

            if (!systemComponentVersionFromDeploymentLog.equals(databaseSchemaVersion.getVersion())) {
                return RuleResult.failed("Database schema version '" + databaseSchemaVersion.getVersion()
                        + "' published to the architecture repository does not match the currently deployed service version '" + systemComponentVersionFromDeploymentLog + "'.");
            }

            return RuleResult.ok("Database schema in the architecture repository matches the currently deployed service version");
        }

        return RuleResult.ok("Database schema found in the architecture repository");

    }

    private boolean ignoreComponent(RuleParameters ruleParameters, String serviceName) {
        if (!ruleParameters.parameters().isEmpty()) {
            for (String ignoredServiceName : ruleParameters.getParameterAsList("ignored-service-names")) {
                if (serviceName.contains(ignoredServiceName.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

}
