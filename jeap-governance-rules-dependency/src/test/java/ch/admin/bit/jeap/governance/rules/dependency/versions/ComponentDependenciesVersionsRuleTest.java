package ch.admin.bit.jeap.governance.rules.dependency.versions;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentDependenciesVersionsRuleTest {

    @Mock
    private PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @InjectMocks
    private ComponentDependenciesVersionsRule rule;

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-dependencies-versions");
        assertThat(metadata.label()).isEqualTo("Component Dependencies Versions");
    }

    @Test
    void noParameters() {
        //given
        final RuleParameters emptyParams = new RuleParameters(Map.of());
        SystemComponent component = mockComponent();

        //when
        var e = assertThrows(IllegalArgumentException.class,
                () -> rule.evaluate(component, emptyParams));

        //then
        assertThat(e.getMessage()).isEqualTo("No dependencies provided to check, please provide at least one dependency with the format 'groupId:artifactId:minimumVersion' or 'artifactId:minimumVersion'");
    }

    @Test
    void noDependencyVersionInformation() {
        //given
        SystemComponent component = mockComponent();

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("versions", List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.2.0",
                "ch.admin.bit.jeap:jeap-messaging:9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-outbox:9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-sequential-inbox:9.3.1",
                "ch.admin.bit.jeap:jeap-crypto:4.2.0",
                "ch.admin.bit.jeap:jeap-error-handling-service:14.0.0",
                "ch.admin.bit.jeap:jeap-process-context-scs:13.25.0",
                "ch.admin.bit.jeap:jeap-process-archive-service:9.5.0",
                "spring.boot:3.5.6")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No dependency version information available");
    }

    @Test
    void ignoreInvalidMinimumVersion() {
        //given
        SystemComponent component = mockComponent();

        //when
        RuleResult result = rule.evaluate(component, new RuleParameters(Map.of(
                "versions", "foo:bar:invalid:1.2.3")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No dependency version information available");
    }

    @Test
    void dependenciesUpToDate() {
        //given
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.SELF_CONTAINED_SYSTEM).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(createOkSamples(component));

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("versions", List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.2.0",
                "ch.admin.bit.jeap:jeap-messaging:9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-outbox:9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-sequential-inbox:9.3.1",
                "ch.admin.bit.jeap:jeap-crypto:4.2.0",
                "ch.admin.bit.jeap:jeap-error-handling-service:14.0.0",
                "ch.admin.bit.jeap:jeap-process-context-scs:13.25.0",
                "ch.admin.bit.jeap:jeap-process-archive-service:9.5.0",
                "spring.boot:3.5.6")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);

        List<String> expectedComments = List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter is up to date with version 18.2.0",
                "ch.admin.bit.jeap:jeap-messaging is up to date with version 9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-outbox is up to date with version 9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-sequential-inbox is up to date with version 9.3.1",
                "ch.admin.bit.jeap:jeap-crypto is up to date with version 4.2.0",
                "ch.admin.bit.jeap:jeap-error-handling-service is up to date with version 14.0.0",
                "ch.admin.bit.jeap:jeap-process-context-scs is up to date with version 13.25.0",
                "ch.admin.bit.jeap:jeap-process-archive-service is up to date with version 9.5.0",
                "spring.boot is up to date with version 3.5.6"
        );

        assertThat(Arrays.asList(result.stateComment().split("; "))).containsExactlyInAnyOrderElementsOf(expectedComments);
    }

    @Test
    void dependenciesNotUpToDate() {
        //given
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.SELF_CONTAINED_SYSTEM).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(createNokSamples(component));

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("versions", List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.2.0",
                "ch.admin.bit.jeap:jeap-messaging:9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-outbox:9.3.1",
                "ch.admin.bit.jeap:jeap-messaging-sequential-inbox:9.3.1",
                "ch.admin.bit.jeap:jeap-crypto:4.2.0",
                "ch.admin.bit.jeap:jeap-error-handling-service:14.0.0",
                "ch.admin.bit.jeap:jeap-process-context-scs:13.25.0",
                "ch.admin.bit.jeap:jeap-process-archive-service:9.5.0",
                "spring.boot:3.5.6")));

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);

        List<String> expectedComments = List.of(
                "ch.admin.bit.jeap:jeap-messaging-sequential-inbox is outdated with version 9.3.0, please update to at least version 9.3.1",
                "ch.admin.bit.jeap:jeap-crypto is outdated with version 4.1.0, please update to at least version 4.2.0",
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter is outdated with version 18.1.0, please update to at least version 18.2.0",
                "ch.admin.bit.jeap:jeap-messaging-outbox is outdated with version 9.3.0, please update to at least version 9.3.1",
                "ch.admin.bit.jeap:jeap-messaging is outdated with version 9.3.0, please update to at least version 9.3.1",
                "ch.admin.bit.jeap:jeap-process-archive-service is outdated with version 9.4.0, please update to at least version 9.5.0",
                "spring.boot is outdated with version 3.5.5, please update to at least version 3.5.6",
                "ch.admin.bit.jeap:jeap-error-handling-service is outdated with version 13.9.0, please update to at least version 14.0.0",
                "ch.admin.bit.jeap:jeap-process-context-scs is outdated with version 13.24.1, please update to at least version 13.25.0"
        );

        assertThat(Arrays.asList(result.stateComment().split("; "))).containsExactlyInAnyOrderElementsOf(expectedComments);
    }

    @Test
    void oneDependencyNotUpToDate() {
        //given
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.SELF_CONTAINED_SYSTEM).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(createOkSamples(component));

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("versions", List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.2.0",
                "ch.admin.bit.jeap:jeap-messaging:10.9.1",
                "ch.admin.bit.jeap:jeap-crypto:3.6.0")));

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);

        List<String> expectedComments = List.of(
                "ch.admin.bit.jeap:jeap-crypto is up to date with version 4.2.0",
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter is up to date with version 18.2.0",
                "ch.admin.bit.jeap:jeap-messaging is outdated with version 9.3.1, please update to at least version 10.9.1"
        );

        assertThat(Arrays.asList(result.stateComment().split("; "))).containsExactlyElementsOf(expectedComments);
    }

    @Test
    void duplicateDependencies() {
        //given
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.SELF_CONTAINED_SYSTEM).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(
                List.of(
                        createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter", "18.1.0"),
                        createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter", "18.1.0")
                )
        );

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("versions", List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.2.0")));

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);

        List<String> expectedComments = List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter is outdated with version 18.1.0, please update to at least version 18.2.0"
        );

        assertThat(Arrays.asList(result.stateComment().split("; "))).containsExactlyElementsOf(expectedComments);
    }

    @Test
    void invalidDependency() {
        //given
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.SELF_CONTAINED_SYSTEM).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(
                List.of(
                        createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter", "18.1.0-SNAPSHOT"),
                        createSample(component, "ch.admin.bit.jeap:jeap-crypto", "foo.bar")
                )
        );

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("versions", List.of("ch.admin.bit.jeap:jeap-spring-boot-application-starter:18.0.0", "ch.admin.bit.jeap:jeap-crypto:1.0.0")));

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);

        List<String> expectedComments = List.of(
                "ch.admin.bit.jeap:jeap-spring-boot-application-starter has an invalid version: Component uses Snapshot version: 18.1.0-SNAPSHOT",
                "ch.admin.bit.jeap:jeap-crypto has an invalid version: Component seems to use an un-parsable version number: foo.bar"
        );

        assertThat(Arrays.asList(result.stateComment().split("; "))).containsExactlyInAnyOrderElementsOf(expectedComments);
    }

    private List<PromTimeSeries> createOkSamples(SystemComponent component) {
        return List.of(
                createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter", "18.2.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging", "9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-outbox", "9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-sequential-inbox", "9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-crypto", "4.2.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-error-handling-service", "14.0.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-context-scs", "13.25.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-archive-service", "9.5.0"),
                createSample(component, "spring.boot", "3.5.6")
        );
    }

    private List<PromTimeSeries> createNokSamples(SystemComponent component) {
        return List.of(
                createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-application-starter", "18.1.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging", "9.3.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-outbox", "9.3.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-messaging-sequential-inbox", "9.3.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-crypto", "4.1.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-error-handling-service", "13.9.0"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-context-scs", "13.24.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-process-archive-service", "9.4.0"),
                createSample(component, "spring.boot", "3.5.5")
        );
    }

    private PromTimeSeries createSample(SystemComponent component, String name, String version) {
        return PromTimeSeries.builder()
                .prometheusQueryType(PromQueryType.JEAP_DEPENDENCY_VERSION)
                .queryTimestamp(ZonedDateTime.now())
                .systemComponentId(component.getId())
                .sample(new PromTimeSeriesSample(Map.of("name", name, "version", version), List.of("1")))
                .build();
    }

    private SystemComponent mockComponent() {
        return mock(SystemComponent.class);
    }

}
