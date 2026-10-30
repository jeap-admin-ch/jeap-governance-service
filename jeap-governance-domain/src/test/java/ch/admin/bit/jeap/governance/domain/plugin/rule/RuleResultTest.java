package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation") // also covers the deprecated summarize(List)
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

    @Test
    void failedWithItems_oneItem() {
        RuleResult result = RuleResult.failed("Unprotected endpoints:", List.of("'GET /a'"));

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Unprotected endpoints:\n'GET /a'");
    }

    @Test
    void failedWithItems_oneItemPerLine() {
        RuleResult result = RuleResult.failed("Unprotected endpoints:", List.of("'GET /a'", "'POST /b'", "'PUT /c'"));

        assertThat(result.stateComment().lines().toList())
                .containsExactly("Unprotected endpoints:", "'GET /a'", "'POST /b'", "'PUT /c'");
    }

    @Test
    void failedWithItems_blankItemsAreLeftOut() {
        List<String> items = new ArrayList<>(List.of("'GET /a'", "", "   "));
        items.add(null);

        RuleResult result = RuleResult.failed("Unprotected endpoints:", items);

        assertThat(result.stateComment()).isEqualTo("Unprotected endpoints:\n'GET /a'");
    }

    @Test
    void failedWithItems_summaryOnly() {
        RuleResult result = RuleResult.failed("Unprotected endpoints:", List.of());

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Unprotected endpoints:");
    }

    @Test
    void failedWithItems_lineBreaksWithinAnItemOrTheSummaryAreReplaced() {
        RuleResult result = RuleResult.failed("Unprotected\nendpoints:", List.of(" 'GET /a'\r\n  first ", "'POST /b'"));

        assertThat(result.stateComment().lines().toList())
                .containsExactly("Unprotected endpoints:", "'GET /a' first", "'POST /b'");
    }

    @Test
    void summarizeWithSummary_listsTheFailingResultsOnly() {
        List<RuleResult> results = List.of(
                RuleResult.ok("a is up to date"),
                RuleResult.failed("b is outdated"),
                RuleResult.failed(),
                RuleResult.failed("c is outdated"));

        RuleResult result = RuleResult.summarize("Outdated dependencies:", results);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Outdated dependencies:\nb is outdated\nc is outdated");
        assertThat(result.stateComment()).doesNotContain("; ");
    }

    @Test
    void summarizeWithSummary_multiLineFailureCommentsBecomeSeveralItems() {
        RuleResult result = RuleResult.summarize("Violations:",
                List.of(RuleResult.failed("first\nsecond"), RuleResult.failed("third")));

        assertThat(result.stateComment().lines().toList()).containsExactly("Violations:", "first", "second", "third");
    }

    @Test
    void summarizeWithSummary_allOk_isOkWithoutComment() {
        RuleResult result = RuleResult.summarize("Outdated dependencies:",
                List.of(RuleResult.ok("a is up to date"), RuleResult.ok()));

        assertThat(result).isEqualTo(RuleResult.ok());
    }

    @Test
    void summarizeWithSummary_noResults_isOkWithoutComment() {
        assertThat(RuleResult.summarize("Outdated dependencies:", List.of())).isEqualTo(RuleResult.ok());
    }
}
