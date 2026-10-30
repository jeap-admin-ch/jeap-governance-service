package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Shared by the rule modules: every rule bean a module ships must carry its explanation, see
 * {@link RuleExplanation}. A rule added without one fails its module's build.
 * <p>
 * The rule beans are found by scanning the module's package for {@link Component}s implementing {@link Rule}, so a
 * new rule is covered without being listed anywhere. Each is created with mocked collaborators, which is enough to
 * read its metadata.
 */
public final class RuleExplanationCoverage {

    private RuleExplanationCoverage() {
    }

    /**
     * @param basePackage   the package of the module's rules
     * @param expectedRules the number of rule beans expected in the package; guards against a scan that silently
     *                      finds nothing
     */
    public static void assertEveryRuleHasAnExplanation(String basePackage, int expectedRules) {
        List<Rule> rules = findRuleBeans(basePackage);
        assertThat(rules)
                .withFailMessage("Expected %d rule beans in %s, found %s", expectedRules, basePackage, rules)
                .hasSize(expectedRules);

        List<String> withoutExplanation = new ArrayList<>();
        for (Rule rule : rules) {
            RuleId ruleId = rule.metadata().ruleId();
            String explanation = RuleExplanation.load(ruleId, rule.getClass().getClassLoader()).orElse(null);
            if (explanation == null || explanation.isBlank()) {
                withoutExplanation.add(rule.getClass().getSimpleName() + " (" + RuleExplanation.resourcePath(ruleId) + ")");
            }
        }
        assertThat(withoutExplanation)
                .withFailMessage("Every rule must ship its explanation as a Markdown resource, these do not: %s",
                        withoutExplanation)
                .isEmpty();
    }

    private static List<Rule> findRuleBeans(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, new StandardEnvironment());
        scanner.addIncludeFilter(new AssignableTypeFilter(Rule.class));
        return scanner.findCandidateComponents(basePackage).stream()
                .map(definition -> loadClass(definition.getBeanClassName()))
                .filter(type -> type.isAnnotationPresent(Component.class))
                .filter(type -> !Modifier.isAbstract(type.getModifiers()))
                .sorted(Comparator.comparing(Class::getName))
                .map(RuleExplanationCoverage::instantiateWithMocks)
                .toList();
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Rule instantiateWithMocks(Class<?> type) {
        Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(RuleExplanationCoverage::mockOf)
                .toArray();
        try {
            constructor.setAccessible(true);
            return (Rule) constructor.newInstance(arguments);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create the rule " + type.getName() + " to read its metadata", e);
        }
    }

    private static Object mockOf(Class<?> parameterType) {
        if (parameterType == List.class) {
            return List.of();
        }
        return mock(parameterType);
    }
}
