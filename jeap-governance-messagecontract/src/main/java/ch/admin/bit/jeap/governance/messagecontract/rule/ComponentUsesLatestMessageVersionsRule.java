package ch.admin.bit.jeap.governance.messagecontract.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.*;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComponentUsesLatestMessageVersionsRule implements Rule {
    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-uses-latest-message-versions"))
            .label("Component Uses Latest Message Versions")
            .build();

    private final MessageContractVersionStatusRepository repository;

    @Override
    public RuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public RuleResult evaluate(SystemComponent component, RuleParameters parameters) {
        if (component.getType() != ComponentType.BACKEND_SERVICE
                && component.getType() != ComponentType.SELF_CONTAINED_SYSTEM) {
            return RuleResult.ok("Not applicable");
        }
        var outdated = repository.findOutdatedByAppName(component.getName());
        if (outdated.isEmpty()) {
            return RuleResult.ok("No outdated message contracts found");
        }
        String details = outdated.stream()
                .sorted(Comparator.comparing(MessageContractVersionStatus::getMessageType)
                        .thenComparing(MessageContractVersionStatus::getRole)
                        .thenComparing(MessageContractVersionStatus::getTopic))
                .map(status -> "%s (%s on %s) uses %s, latest is %s".formatted(status.getMessageType(),
                        status.getRole(), status.getTopic(), status.getUsedVersion(), status.getLatestVersion()))
                .collect(Collectors.joining("\n"));
        return RuleResult.failed("Outdated message contracts:\n" + details);
    }
}
