package ch.admin.bit.jeap.governance.messagecontract.domain;

/** Result of comparing a deployed contract's message type version with the latest version in its registry. */
public enum MessageContractVersionState {
    UP_TO_DATE,
    OUTDATED,
    /**
     * The Message Contract Service could not determine the latest version for this contract, e.g. because its message
     * type is missing from the registry. Such a contract can be evaluated neither as up to date nor as outdated.
     */
    UNKNOWN
}
