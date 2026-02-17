package ch.admin.bit.jeap.governance.domain.rule;

public enum State {
    /**
     * Rule is complied with
     */
    OK,
    /**
     * Rule is violated
     */
    FAIL,
    /**
     * Rule is paused because of a temporary exemption with an end date
     */
    PAUSED,
    /**
     * Rule is disabled because of an exemption
     */
    DISABLED
}
