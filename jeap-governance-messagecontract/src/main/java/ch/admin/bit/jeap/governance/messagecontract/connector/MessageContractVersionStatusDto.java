package ch.admin.bit.jeap.governance.messagecontract.connector;

/**
 * @param latestVersion {@code null} if the status is {@code UNKNOWN}
 * @param status        {@code UP_TO_DATE}, {@code OUTDATED} or {@code UNKNOWN}; {@code null} for Message Contract
 *                      Service versions that do not report it yet
 */
public record MessageContractVersionStatusDto(
        String appName,
        String appVersion,
        String messageType,
        String usedVersion,
        String latestVersion,
        String topic,
        String role,
        boolean upToDate,
        String status) {
}
