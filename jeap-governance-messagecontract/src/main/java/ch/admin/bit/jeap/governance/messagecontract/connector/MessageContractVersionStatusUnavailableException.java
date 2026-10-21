package ch.admin.bit.jeap.governance.messagecontract.connector;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceUnavailableException;

/**
 * The Message Contract Service answered with HTTP 503 because a message type registry could not be read. The previous
 * snapshot is kept and the next import retries.
 */
public class MessageContractVersionStatusUnavailableException extends DataSourceUnavailableException {
    public MessageContractVersionStatusUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
