package ch.admin.bit.jeap.governance.secscan.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpApiExemptions;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.secscan.dataimport.DataimportConfigurationProperties;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpointRepository;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndpointsProtectedRule implements Rule {

    static final String OK_MESSAGE = "No rest endpoint without proper protection detected";
    static final String NO_SCAN_DATA_MESSAGE = "No security scan data available";

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("endpoints-protected"))
            .label("REST Endpoint Security (Scanner)")
            .build();

    private final SecscanFlaggedEndpointRepository flaggedEndpointRepository;
    private final SecscanStateRepository secscanStateRepository;
    private final DataimportConfigurationProperties dataimportConfigurationProperties;

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
        String serviceName = systemComponent.getName();
        HttpApiExemptions exemptions = new HttpApiExemptions(ruleParameters);

        if (exemptions.shouldExemptComponent(serviceName).exempted()) {
            return RuleResult.ok(OK_MESSAGE);
        }

        Optional<SecscanState> secscanState = secscanStateRepository.findBySystemComponentId(systemComponent.getId());
        if (secscanState.isEmpty()) {
            return RuleResult.failed(NO_SCAN_DATA_MESSAGE);
        }

        List<SecscanFlaggedEndpoint> flaggedEndpoints = flaggedEndpointRepository.findBySystemComponentId(systemComponent.getId());
        if (flaggedEndpoints.isEmpty()) {
            return RuleResult.ok(OK_MESSAGE);
        }

        String environment = dataimportConfigurationProperties.getTargetEnvironment().name();
        List<String> failedEndpointMessages = new ArrayList<>();

        for (SecscanFlaggedEndpoint flaggedEndpoint : flaggedEndpoints) {
            String path = flaggedEndpoint.getPath();
            String method = flaggedEndpoint.getMethod();
            if (!exemptions.shouldExemptHttpEndpoint(new HttpEndpoint(path, method), environment).exempted()) {
                String endpointId = method + " " + path;
                String message = "Endpoint '" + endpointId + "' not properly protected"
                        + (flaggedEndpoint.getScanMessage() != null ? " (" + flaggedEndpoint.getScanMessage() + ")" : "");
                log.info("Component '{}': {}", serviceName, message);
                failedEndpointMessages.add(message);
            }
        }

        if (failedEndpointMessages.isEmpty()) {
            return RuleResult.ok(OK_MESSAGE);
        }

        // Sort to get a repeatable, predictable result
        Collections.sort(failedEndpointMessages);
        return RuleResult.failed(String.join("; ", failedEndpointMessages));
    }

}
