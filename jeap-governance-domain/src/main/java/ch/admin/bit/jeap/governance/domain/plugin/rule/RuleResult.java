package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The result of evaluating a rule for a component.
 * <p>
 * The state comment is plain text, not Markdown: its first line is the summary, and every further line is one
 * item, for instance one violation. Readers show the summary as text and the items as a list. Build comments that
 * list several items with {@link #failed(String, List)} or {@link #summarize(String, List)} rather than joining the
 * items yourself.
 */
public record RuleResult(State state, String stateComment) {

    public static RuleResult ok() {
        return new RuleResult(State.OK, null);
    }

    public static RuleResult ok(String stateComment) {
        return new RuleResult(State.OK, stateComment);
    }

    public static RuleResult failed() {
        return new RuleResult(State.FAIL, null);
    }

    public static RuleResult failed(String stateComment) {
        return new RuleResult(State.FAIL, stateComment);
    }

    /**
     * A failed result whose comment is the summary followed by one item per line. Blank items are left out, and
     * line breaks within the summary or an item are replaced by spaces, so that every item stays one line.
     *
     * @param summary the first line of the comment, e.g. {@code "Unprotected endpoints:"}
     * @param items   the items, e.g. the violations
     */
    public static RuleResult failed(String summary, List<String> items) {
        return failed(comment(summary, items));
    }

    /**
     * Combines the results of several checks: failed if any of them failed, with the summary followed by the
     * comment lines of the failing results as items. OK results contribute no items, and the combined result of
     * checks that all passed carries no comment.
     *
     * @param summary the first line of the comment of a failed result, e.g. {@code "Outdated dependencies:"}
     * @param results the results of the single checks
     */
    public static RuleResult summarize(String summary, List<RuleResult> results) {
        List<RuleResult> failures = results.stream()
                .filter(result -> result.state() == State.FAIL)
                .toList();
        if (failures.isEmpty()) {
            return ok();
        }
        List<String> items = failures.stream()
                .map(RuleResult::stateComment)
                .filter(Objects::nonNull)
                .flatMap(String::lines)
                .toList();
        return failed(summary, items);
    }

    /**
     * @deprecated joins the comments with {@code "; "} into a single line, which readers cannot show as a list. Use
     * {@link #summarize(String, List)} instead.
     */
    @Deprecated(since = "10.0.0")
    public static RuleResult summarize(List<RuleResult> results) {
        boolean hasFailure = results.stream().anyMatch(result -> result.state().equals(State.FAIL));
        String stateComment = results.stream().map(RuleResult::stateComment).filter(Objects::nonNull).collect(Collectors.joining("; "));
        return hasFailure ? RuleResult.failed(stateComment) : RuleResult.ok(stateComment);
    }

    private static String comment(String summary, List<String> items) {
        List<String> lines = new ArrayList<>();
        lines.add(singleLine(Objects.requireNonNull(summary, "summary")));
        items.stream()
                .filter(Objects::nonNull)
                .map(RuleResult::singleLine)
                .filter(item -> !item.isEmpty())
                .forEach(lines::add);
        return String.join("\n", lines);
    }

    private static String singleLine(String text) {
        return text.strip().replaceAll("\\s*\\R\\s*", " ");
    }
}
