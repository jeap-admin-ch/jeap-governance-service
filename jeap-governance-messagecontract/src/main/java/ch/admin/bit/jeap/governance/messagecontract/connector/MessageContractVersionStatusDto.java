package ch.admin.bit.jeap.governance.messagecontract.connector;

public record MessageContractVersionStatusDto(
        String appName,
        String appVersion,
        String messageType,
        String usedVersion,
        String latestVersion,
        String topic,
        String role,
        boolean upToDate) {
}
