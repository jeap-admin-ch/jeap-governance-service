package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleResultTest {

    @Test
    void summarize_shouldReturnOk_whenAllResultsAreOk() {
        List<RuleResult> results = List.of(
                RuleResult.ok("comment1"),
                RuleResult.ok("comment2")
        );

        RuleResult result = RuleResult.summarize(results);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("comment1; comment2");
    }

    @Test
    void summarize_shouldReturnFailed_whenAnyResultFails() {
        List<RuleResult> results = List.of(
                RuleResult.ok("comment1"),
                RuleResult.failed("error"),
                RuleResult.ok("comment2")
        );

        RuleResult result = RuleResult.summarize(results);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("comment1; error; comment2");
    }

    @Test
    void summarize_shouldFilterNullComments() {
        List<RuleResult> results = List.of(
                RuleResult.ok(),
                RuleResult.failed("error"),
                RuleResult.ok("comment")
        );

        RuleResult result = RuleResult.summarize(results);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("error; comment");
    }

    @Test
    void summarize_shouldReturnEmptyComment_whenNoComments() {
        List<RuleResult> results = List.of(
                RuleResult.ok(),
                RuleResult.ok()
        );

        RuleResult result = RuleResult.summarize(results);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEmpty();
    }

    @Test
    void summarize_shouldReturnOk_whenEmpty() {
        List<RuleResult> results = List.of();

        RuleResult result = RuleResult.summarize(results);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEmpty();
    }

}
