package ch.admin.bit.jeap.governance.rules.core.naming;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentNamingConventionRuleTest {

    private final ComponentNamingConventionRule rule = new ComponentNamingConventionRule();
    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-naming-convention");
        assertThat(metadata.label()).isEqualTo("Component Naming Convention");
    }

    @ParameterizedTest
    @MethodSource("validNameCases")
    void validName(String systemName, Set<String> aliases, String componentName) {
        var component = createComponentWithAliases(systemName, aliases, componentName);
        var result = rule.evaluate(component, emptyParams);
        assertThat(result.state()).isEqualTo(State.OK);
    }

    static Stream<Arguments> validNameCases() {
        return Stream.of(
                Arguments.of("mysystem", Set.of(), "mysystem-context-service"),
                Arguments.of("mysystem", Set.of(), "mysystem-admin-ui"),
                Arguments.of("mysystem", Set.of(), "mysystem-portal-scs"),
                Arguments.of("mysystem", Set.of(), "mysystem-user-admin-panel-service"),
                Arguments.of("mysystem", Set.of("myalias"), "myalias-context-service"),
                Arguments.of("MySystem", Set.of(), "mysystem-context-service")
        );
    }

    @ParameterizedTest
    @MethodSource("failCases")
    void fail(String systemName, String componentName, String expectedComment) {
        var component = createComponent(systemName, componentName);
        var result = rule.evaluate(component, emptyParams);
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).contains(expectedComment);
    }

    static Stream<Arguments> failCases() {
        return Stream.of(
                Arguments.of("mysystem", "mysystem-service", "at least 3 parts"),
                Arguments.of("mysystem", "mysystem", "at least 3 parts"),
                Arguments.of("mysystem", "mysystem-context-webapp", "Type-id 'webapp' is not valid"),
                Arguments.of("mysystem", "other-context-service", "does not match system name"),
                Arguments.of("mysystem", "123invalid-context-service", "must match pattern"),
                Arguments.of("mysystem", "mysystem-123invalid-service", "Context part")
        );
    }

    @Test
    void fail_multipleViolations() {
        var component = createComponent("mysystem", "123bad-456BAD-webapp");
        var result = rule.evaluate(component, emptyParams);
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).contains("must match pattern");
        assertThat(result.stateComment()).contains("Type-id 'webapp' is not valid");
    }

    @Test
    void validName_allTypeIds() {
        for (String typeId : List.of("service", "ui", "scs", "mobileapp", "gateway", "db")) {
            var component = createComponent("mysystem", "mysystem-context-" + typeId);
            var result = rule.evaluate(component, emptyParams);
            assertThat(result.state()).as("type-id: %s", typeId).isEqualTo(State.OK);
        }
    }

    private SystemComponent createComponent(String systemName, String componentName) {
        return createComponentWithAliases(systemName, Set.of(), componentName);
    }

    private SystemComponent createComponentWithAliases(String systemName, Set<String> aliases, String componentName) {
        var component = SystemComponent.builder()
                .name(componentName)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System.builder()
                .name(systemName)
                .aliases(aliases)
                .systemComponents(List.of(component))
                .build();
        return component;
    }
}
