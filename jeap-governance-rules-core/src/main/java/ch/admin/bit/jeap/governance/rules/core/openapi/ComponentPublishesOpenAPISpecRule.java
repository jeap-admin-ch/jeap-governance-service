package ch.admin.bit.jeap.governance.rules.core.openapi;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersionRepository;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean({ApiDocVersionRepository.class, DeploymentLogComponentVersionRepository.class})
@RequiredArgsConstructor
public class ComponentPublishesOpenAPISpecRule implements Rule {

    private final ApiDocVersionRepository apiDocVersionRepository;
    private final DeploymentLogComponentVersionRepository deploymentLogComponentVersionRepository;

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-publishes-openapispec"))
            .label("Component Publishes OpenAPI Spec")
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

        Optional<ApiDocVersion> archRepoApiDocVersionOptional = apiDocVersionRepository.findByComponentId(systemComponent.getId());
        if (archRepoApiDocVersionOptional.isEmpty()) {
            return RuleResult.failed("No OpenAPI specification found in the architecture repository, it has to be uploaded by the application");
        }

        ApiDocVersion archRepoApiDocVersion = archRepoApiDocVersionOptional.get();
        Optional<DeploymentLogComponentVersion> deploymentLogComponentVersionOptional = deploymentLogComponentVersionRepository.findByComponentId(systemComponent.getId());

        if (deploymentLogComponentVersionOptional.isPresent()) {
            String systemComponentVersionFromDeploymentLog = deploymentLogComponentVersionOptional.get().getVersion();

            if (!systemComponentVersionFromDeploymentLog.equals(archRepoApiDocVersion.getVersion())) {
                return RuleResult.failed("OpenAPI specification version '" + archRepoApiDocVersion.getVersion()
                        + "' published to the architecture repository does not match the currently deployed service version '" + systemComponentVersionFromDeploymentLog + "'.");
            }
            return RuleResult.ok("OpenAPI specification in the architecture repository matches the currently deployed service version");
        }
        return RuleResult.ok("OpenAPI specification found in the architecture repository");
    }
}
