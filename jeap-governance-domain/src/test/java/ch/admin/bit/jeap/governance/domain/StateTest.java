package ch.admin.bit.jeap.governance.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StateTest {

    @Test
    void merge_okAndOk() {
        assertEquals(State.OK, State.merge(State.OK, State.OK));
    }

    @Test
    void merge_okAndFail() {
        assertEquals(State.FAIL, State.merge(State.OK, State.FAIL));
    }

    @Test
    void merge_failAndOk() {
        assertEquals(State.FAIL, State.merge(State.FAIL, State.OK));
    }

    @Test
    void merge_ignoreAndIgnore() {
        assertEquals(State.IGNORE, State.merge(State.IGNORE, State.IGNORE));
    }

    @Test
    void merge_okAndIgnore() {
        assertEquals(State.OK, State.merge(State.OK, State.IGNORE));
    }

    @Test
    void merge_ignoreAndOk() {
        assertEquals(State.OK, State.merge(State.IGNORE, State.OK));
    }

    @Test
    void merge_okAndUnknown() {
        assertEquals(State.UNKNOWN, State.merge(State.OK, State.UNKNOWN));
    }

    @Test
    void merge_unknownAndOk() {
        assertEquals(State.UNKNOWN, State.merge(State.UNKNOWN, State.OK));
    }

}
