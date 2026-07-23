package ch.admin.bit.jeap.governance.rules.messaging.contracts;

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
public class ComponentDefinesMessagingContractsRule implements Rule {

    private static final String SWITCH_NAME_TAG = "switch";
    private static final String NO_MASTER_SWITCH_NAME = "noMasterContracts";
    private static final String CONSUME_WITHOUT_SWITCH_NAME = "consumeWithoutContract";
    private static final String PUBLISH_WITHOUT_SWITCH_NAME = "publishWithoutContract";
    private static final String SILENT_IGNORE_WITHOUT_SWITCH_NAME = "silentIgnoreWithoutContract";

    private final PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-defines-messagingcontracts"))
            .label("Component Defines Messaging Contracts")
            .build();

    @Override
    public RuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
        List<PromTimeSeries> prometheusQueryResponses = promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_CONTRACT, systemComponent.getId());
        if (prometheusQueryResponses.isEmpty()) {
            return RuleResult.ok("No messaging library detected");
        }
        if (getSwitchValue(prometheusQueryResponses, NO_MASTER_SWITCH_NAME)) {
            return RuleResult.failed("Contracts from branched other than master");
        }
        if (getSwitchValue(prometheusQueryResponses, CONSUME_WITHOUT_SWITCH_NAME)) {
            return RuleResult.failed("Consume without contracts enabled");
        }
        if (getSwitchValue(prometheusQueryResponses, PUBLISH_WITHOUT_SWITCH_NAME)) {
            return RuleResult.failed("Publish without contracts enabled");
        }
        if (getSwitchValue(prometheusQueryResponses, SILENT_IGNORE_WITHOUT_SWITCH_NAME)) {
            return RuleResult.failed("Silently ignoring messages without contract enabled");
        }
        return RuleResult.ok("Contracts enabled");
    }

    private boolean getSwitchValue(List<PromTimeSeries> prometheusQueryResponses, String switchName) {
        return prometheusQueryResponses.stream()
                .filter(x -> switchName.equals(x.getSample().metric().get(SWITCH_NAME_TAG)))
                //There might be several results. If one is true we take that one
                .anyMatch(result -> result.getSample().value().stream().anyMatch(val -> val.equalsIgnoreCase("1")));
    }

}
