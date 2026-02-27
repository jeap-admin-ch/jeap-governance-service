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
public class ComponentProducesSignedMessagesRule implements Rule {

    private static final String SIGNED_NAME_TAG = "signed";
    private static final String TYPE_NAME_TAG = "type";

    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @Override
    public RuleMetadata metadata() {
        return RuleMetadata.builder()
                .ruleId(RuleId.of("component-produces-signedmessages"))
                .label("Component Produces Signed Messages")
                .build();
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        List<PromTimeSeries> messagingTotalQueryResponses = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, systemComponent.getId());
        if (messagingTotalQueryResponses.isEmpty()) {
            return RuleResult.ok("No messaging library detected");
        }
        if (hasProducedUnsignedMessages(messagingTotalQueryResponses)) {
            return RuleResult.failed("Publisher sends unsigned messages");
        }
        return RuleResult.ok("All messages are signed");
    }

    private boolean hasProducedUnsignedMessages(List<PromTimeSeries> prometheusQueryResponses) {
        String producer = "producer";
        // "1" indicates that the message was signed
        String signedOne = "1";

        return prometheusQueryResponses.stream()
                .filter(x -> producer.equals(x.getSample().metric().get(TYPE_NAME_TAG)))
                .anyMatch(x -> !signedOne.equals(x.getSample().metric().get(SIGNED_NAME_TAG)));
    }
}
