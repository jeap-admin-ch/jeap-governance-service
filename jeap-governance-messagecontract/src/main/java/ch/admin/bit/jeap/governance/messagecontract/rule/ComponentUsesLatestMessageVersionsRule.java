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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComponentUsesLatestMessageVersionsRule implements Rule {
    private static final RuleMetadata METADATA = RuleMetadata.builder()
            .ruleId(RuleId.of("component-uses-latest-message-versions"))
            .label("Component Uses Latest Message Versions")
            .build();
    private static final Comparator<MessageContractVersionStatus> ORDER =
            Comparator.comparing(MessageContractVersionStatus::getMessageType)
                    .thenComparing(MessageContractVersionStatus::getRole)
                    .thenComparing(MessageContractVersionStatus::getTopic);

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
        // Contracts with an unknown latest version cannot be evaluated: they are neither reported as violation nor
        // as compliant, but listed in the comment so that the team can fix them.
        Map<Boolean, List<MessageContractVersionStatus>> unknownOrOutdated = repository
                .findNotUpToDateByAppName(component.getName()).stream()
                .sorted(ORDER)
                .collect(Collectors.partitioningBy(MessageContractVersionStatus::isUnknown));
        List<MessageContractVersionStatus> outdated = unknownOrOutdated.get(false);
        List<MessageContractVersionStatus> unknown = unknownOrOutdated.get(true);

        String unknownDetails = unknown.isEmpty() ? "" : "Message contracts with unknown latest version (not evaluated):\n"
                + unknown.stream()
                .map(status -> "%s (%s on %s) uses %s".formatted(status.getMessageType(), status.getRole(),
                        status.getTopic(), status.getUsedVersion()))
                .collect(Collectors.joining("\n"));
        if (outdated.isEmpty()) {
            return RuleResult.ok(unknown.isEmpty() ? "No outdated message contracts found"
                    : "No outdated message contracts found\n" + unknownDetails);
        }
        String details = outdated.stream()
                .map(status -> "%s (%s on %s) uses %s, latest is %s".formatted(status.getMessageType(),
                        status.getRole(), status.getTopic(), status.getUsedVersion(), status.getLatestVersion()))
                .collect(Collectors.joining("\n"));
        return RuleResult.failed("Outdated message contracts:\n" + details
                + (unknown.isEmpty() ? "" : "\n" + unknownDetails));
    }
}
