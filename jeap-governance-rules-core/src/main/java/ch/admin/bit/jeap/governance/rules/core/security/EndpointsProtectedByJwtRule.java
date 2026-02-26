package ch.admin.bit.jeap.governance.rules.core.security;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpApiExemptions;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.mapping;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(PromTimeSeriesQueryRepository.class)
public class EndpointsProtectedByJwtRule implements Rule {

    private static final String OK_MESSAGE = "No rest endpoint without JWT bearer token protection detected";

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("endpoints-protected-by-jwt"))
            .label("REST Endpoint Security (Monitoring)")
            .build();

    private final PromTimeSeriesQueryRepository repository;

    @Override
    public RuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public void validateParameters(RuleParameters ruleParameters) {
        HttpApiExemptions.validateParameters(ruleParameters);
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {

        List<PromTimeSeries> timeSeriesList = repository.findBy(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT, systemComponent.getId());

        if (timeSeriesList.isEmpty()) {
            return RuleResult.ok(OK_MESSAGE);
        }

        String serviceName = systemComponent.getName();
        HttpApiExemptions exemptions = new HttpApiExemptions(ruleParameters);

        if (exemptions.shouldExemptComponent(serviceName).exempted()) {
            return RuleResult.ok(OK_MESSAGE);
        }

        Set<EndpointEnvironment> endpointEnvironmentSet = new HashSet<>();

        for (PromTimeSeries timeSeries : timeSeriesList) {
            Map<String, String> metric = timeSeries.getSample().metric();
            String path = metric.getOrDefault("datapoint", "");
            String method = metric.getOrDefault("method", "");
            String environment = metric.getOrDefault("stage", "unknown");
            if (!exemptions.shouldExemptHttpEndpoint(new HttpEndpoint(path, method), environment).exempted()) {
                endpointEnvironmentSet.add(new EndpointEnvironment(method + " " + path, environment));
            }
        }

        // Group the endpoint-environment set by endpoint and map the environments to a comma separated string.
        // Sort by endpoint and environments to get a repeatable, predictable result.
        Map<String, String> groupedByEndpoint = endpointEnvironmentSet.stream()
                .collect(Collectors.groupingBy(
                        EndpointEnvironment::endpoint,
                        TreeMap::new,
                        mapping(EndpointEnvironment::environment,
                                collectingAndThen(
                                        Collectors.toCollection(TreeSet::new),
                                        envs -> String.join(", ", envs)))));

        if (groupedByEndpoint.isEmpty()) {
            return RuleResult.ok(OK_MESSAGE);
        }

        List<String> endpointMessages = new ArrayList<>();
        groupedByEndpoint.forEach( (endpoint, environments) -> {
            log.info("Component '{}': Detected a call '{}' on environment(s) '{}' without a JWT bearer token.", serviceName, endpoint, environments);
            endpointMessages.add("Call '" + endpoint + "' without a JWT bearer token detected on environment(s) '" + environments + "'");
        });

        return RuleResult.failed(String.join("; ", endpointMessages));
    }

    private record EndpointEnvironment(String endpoint, String environment) {
    }

}
