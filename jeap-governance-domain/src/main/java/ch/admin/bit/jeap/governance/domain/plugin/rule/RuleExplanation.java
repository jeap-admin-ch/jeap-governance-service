package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The explanation of a rule: a Markdown resource shipped next to the rule, found by convention on the classpath at
 * {@code jeap-governance/rules/<rule-id>.md}. It tells the teams whose components are scored what the rule checks
 * and how to comply. The text is delivered unchanged; rendering it safely is up to the consumer.
 */
public final class RuleExplanation {

    private static final String RESOURCE_PATH_PATTERN = "jeap-governance/rules/%s.md";

    private RuleExplanation() {
    }

    /**
     * @return the classpath location of the explanation of the given rule
     */
    public static String resourcePath(RuleId ruleId) {
        return RESOURCE_PATH_PATTERN.formatted(ruleId.id());
    }

    /**
     * @return the explanation of the given rule, read as UTF-8, or empty if the rule ships none
     */
    public static Optional<String> load(RuleId ruleId, ClassLoader classLoader) {
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath(ruleId))) {
            if (inputStream == null) {
                return Optional.empty();
            }
            return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the explanation " + resourcePath(ruleId), e);
        }
    }
}
