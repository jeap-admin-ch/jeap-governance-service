package ch.admin.bit.jeap.governance.rules.dependency.versions;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean(PromTimeSeriesQueryRepository.class)
@RequiredArgsConstructor
public class ComponentDependenciesVersionsRule implements Rule {

    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @Override
    public RuleMetadata metadata() {
        return RuleMetadata.builder()
                .ruleId(RuleId.of("component-dependencies-versions"))
                .label("Component Dependencies Versions")
                .build();
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        if (ruleParameters.parameters().isEmpty()){
            throw new IllegalArgumentException("No dependencies provided to check, please provide at least one dependency with the format 'groupId:artifactId:minimumVersion' or 'artifactId:minimumVersion'");
        }
        List<RuleResult> ruleResults = new ArrayList<>();
        Map<String, String> componentDependencies = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, systemComponent.getId()).stream().collect(Collectors.toMap(
                pqr -> pqr.getSample().metric().get("name"),
                pqr -> pqr.getSample().metric().get("version"),
                (existing, replacement) -> replacement
        ));

        for (Map.Entry<String, String> entry : getVersionsToCheck(ruleParameters).entrySet()) {
            final String dependencyName = entry.getKey();
            try {
                Optional<SemanticVersion> usedVersionOptional = getUsedVersion(componentDependencies, dependencyName);
                if (usedVersionOptional.isEmpty()) {
                    //This dependency is not used at all => we're fine
                    continue;
                }
                SemanticVersion usedVersion = usedVersionOptional.get();
                SemanticVersion minVersion = SemanticVersion.parse(entry.getValue());

                if (usedVersion.compareTo(minVersion) >= 0) {
                    ruleResults.add(RuleResult.ok(dependencyName + " is up to date with version " + usedVersion));
                } else {
                    ruleResults.add(RuleResult.failed(dependencyName + " is outdated with version " + usedVersion + ", please update to at least version " + minVersion));
                }
            } catch (InvalidDependencyVersionException e) {
                ruleResults.add(RuleResult.failed(dependencyName + " has an invalid version: " + e.getMessage()));
            }
        }

        if (ruleResults.isEmpty()) {
            return RuleResult.ok("No dependency version information available");
        }
        return RuleResult.summarize(ruleResults);
    }

    private Map<String, String> getVersionsToCheck(RuleParameters ruleParameters) {
        return ruleParameters.getParameterAsList("versions").stream()
                .map(version -> version.split(":"))
                .filter(split -> split.length == 2 || split.length == 3)
                .collect(Collectors.toMap(
                        split -> split.length == 2
                                ? split[0]
                                : split[0] + ":" + split[1],
                        split -> split.length == 2
                                ? split[1]
                                : split[2]
                ));
    }

    private Optional<SemanticVersion> getUsedVersion(Map<String, String> dependencies, String dependency) {
        if (dependencies.containsKey(dependency)) {
            return Optional.of(SemanticVersion.parse(dependencies.get(dependency)));
        }
        return Optional.empty();
    }
}
