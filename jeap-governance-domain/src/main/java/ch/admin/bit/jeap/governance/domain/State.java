package ch.admin.bit.jeap.governance.domain;

public enum State {
    /**
     * Rule is complied with
     */
    OK,
    /** Rule is violated */
    FAIL,
    /** Rule is ignored because of an exemption */
    IGNORE,
    /** Rule is unknown because it is currently not testable */
    UNKNOWN;

    public static State merge(State state1, State state2) {
        if (state1 == FAIL || state2 == FAIL) {
            return FAIL;
        }
        if (state1 == UNKNOWN || state2 == UNKNOWN) {
            return UNKNOWN;
        }
        if (state1 == IGNORE && state2 == IGNORE) {
            return IGNORE;
        }
        return OK;
    }
}
