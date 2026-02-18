package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RuleStateTest {

    private static final RuleId RULE_ID = RuleId.of("test-rule");
    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2025-01-01T00:00:00Z");
    private static final ZonedDateTime MODIFIED_AT = ZonedDateTime.parse("2025-01-01T00:00:00Z");

    private final SystemComponent component = SystemComponent.builder()
            .name("my-service")
            .state(State.OK)
            .type(ComponentType.BACKEND_SERVICE)
            .build();

    @Test
    void modify_differentState_updatesStateAndModifiedAt() {
        var ruleState = RuleState.createWithTimestamps(RULE_ID, component, State.OK, CREATED_AT, MODIFIED_AT);

        ruleState.modify(State.FAIL, null);

        assertThat(ruleState.getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.getStateComment()).isNull();
        assertThat(ruleState.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(ruleState.getModifiedAt()).isAfter(MODIFIED_AT);
    }

    @Test
    void modify_differentComment_updatesCommentAndModifiedAt() {
        var ruleState = RuleState.createWithTimestamps(RULE_ID, component, State.FAIL, CREATED_AT, MODIFIED_AT);

        ruleState.modify(State.FAIL, "non-compliant");

        assertThat(ruleState.getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.getStateComment()).isEqualTo("non-compliant");
        assertThat(ruleState.getModifiedAt()).isAfter(MODIFIED_AT);
    }

    @Test
    void modify_sameStateAndComment_doesNotUpdateModifiedAt() {
        var ruleState = RuleState.createWithTimestamps(RULE_ID, component, State.OK, CREATED_AT, MODIFIED_AT);

        ruleState.modify(State.OK, null);

        assertThat(ruleState.getState()).isEqualTo(State.OK);
        assertThat(ruleState.getStateComment()).isNull();
        assertThat(ruleState.getModifiedAt()).isEqualTo(MODIFIED_AT);
    }

    @Test
    void modify_differentStateAndComment_updatesBoth() {
        var ruleState = RuleState.createWithTimestamps(RULE_ID, component, State.OK, CREATED_AT, MODIFIED_AT);

        ruleState.modify(State.FAIL, "missing OAuth2");

        assertThat(ruleState.getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.getStateComment()).isEqualTo("missing OAuth2");
        assertThat(ruleState.getModifiedAt()).isAfter(MODIFIED_AT);
    }

    @Test
    void modify_commentChangedFromNonNullToNull_updatesModifiedAt() {
        var ruleState = RuleState.createWithTimestamps(RULE_ID, component, State.FAIL, CREATED_AT, MODIFIED_AT);
        // First set a comment
        ruleState.modify(State.FAIL, "some comment");
        var modifiedAtAfterFirstChange = ruleState.getModifiedAt();

        ruleState.modify(State.FAIL, null);

        assertThat(ruleState.getStateComment()).isNull();
        assertThat(ruleState.getModifiedAt()).isAfterOrEqualTo(modifiedAtAfterFirstChange);
    }

    @Test
    void modify_calledTwiceWithSameValues_doesNotUpdateModifiedAtOnSecondCall() {
        var ruleState = RuleState.createWithTimestamps(RULE_ID, component, State.OK, CREATED_AT, MODIFIED_AT);
        ruleState.modify(State.FAIL, "comment");
        var modifiedAtAfterFirstModify = ruleState.getModifiedAt();

        ruleState.modify(State.FAIL, "comment");

        assertThat(ruleState.getModifiedAt()).isEqualTo(modifiedAtAfterFirstModify);
    }
}
