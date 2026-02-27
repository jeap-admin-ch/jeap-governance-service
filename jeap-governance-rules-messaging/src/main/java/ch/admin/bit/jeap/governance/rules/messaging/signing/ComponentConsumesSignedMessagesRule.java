package ch.admin.bit.jeap.governance.rules.messaging.signing;

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

@Component
@ConditionalOnBean(PromTimeSeriesQueryRepository.class)
@RequiredArgsConstructor
public class ComponentConsumesSignedMessagesRule implements Rule {

    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @Override
    public RuleMetadata metadata() {
        return RuleMetadata.builder()
                .ruleId(RuleId.of("component-consumes-signedmessages"))
                .label("Component Consumes Signed Messages")
                .build();
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        List<PromTimeSeries> messagingTotalQueryResponses = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, systemComponent.getId());
        if (messagingTotalQueryResponses.isEmpty()) {
            return RuleResult.ok("No messaging library detected");
        }

        List<PromTimeSeries> prometheusQueryResponses = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_SIGNATURE_REQUIRED, systemComponent.getId());

        if (prometheusQueryResponses.isEmpty()) {
            //If the result is empty we the signature_required_state metric is not available.
            //So the service is not a messaging consumer.
            return RuleResult.ok("Service is not a messaging consumer");
        }

        if (hasSignatureRequiredStateDisabledAsConsumer(prometheusQueryResponses)) {
            return RuleResult.failed("Signature is not enforced for consumption");
        }

        return RuleResult.ok("Message signature enforcement enabled");
    }

    private boolean hasSignatureRequiredStateDisabledAsConsumer(List<PromTimeSeries> prometheusQueryResponses) {
        return prometheusQueryResponses.stream()
                .flatMap(result -> result.getSample().value().stream())
                .anyMatch(value ->
                        // "1" indicates that signature is required for subscription
                        // "0" indicates that signature is not required
                        value.equals("0")
                );
    }

}
