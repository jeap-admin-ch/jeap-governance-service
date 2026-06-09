package ch.admin.bit.jeap.governance.rules.dependency.webconfig;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentUsesWebConfigStarterRuleTest {

    private static final RuleParameters NO_PARAMS = new RuleParameters(Map.of());

    @Mock
    private PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @InjectMocks
    private ComponentUsesWebConfigStarterRule rule;

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-uses-web-config-starter");
        assertThat(metadata.label()).isEqualTo("Component Uses jEAP Web Config Starter");
    }

    @Test
    void selfContainedSystemUsesWebConfigStarterWithFullCoordinate() {
        //given
        SystemComponent component = scsComponent();
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(List.of(
                createSample(component, "ch.admin.bit.jeap:jeap-messaging", "9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-spring-boot-web-config-starter", "2.1.0")));

        //when
        RuleResult result = rule.evaluate(component, NO_PARAMS);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("jeap-spring-boot-web-config-starter is used");
    }

    @Test
    void selfContainedSystemUsesWebConfigStarterWithBareArtifactId() {
        //given
        SystemComponent component = scsComponent();
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(List.of(
                createSample(component, "jeap-spring-boot-web-config-starter", "2.1.0")));

        //when
        RuleResult result = rule.evaluate(component, NO_PARAMS);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("jeap-spring-boot-web-config-starter is used");
    }

    @Test
    void selfContainedSystemDoesNotUseWebConfigStarter() {
        //given
        SystemComponent component = scsComponent();
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(List.of(
                createSample(component, "ch.admin.bit.jeap:jeap-messaging", "9.3.1"),
                createSample(component, "ch.admin.bit.jeap:jeap-crypto", "4.2.0")));

        //when
        RuleResult result = rule.evaluate(component, NO_PARAMS);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Self-contained system does not use jeap-spring-boot-web-config-starter");
    }

    @Test
    void selfContainedSystemWithoutDependencyInformation() {
        //given
        SystemComponent component = scsComponent();
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, 1L)).thenReturn(List.of());

        //when
        RuleResult result = rule.evaluate(component, NO_PARAMS);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Self-contained system does not use jeap-spring-boot-web-config-starter");
    }

    @Test
    void notApplicableForNonSelfContainedSystem() {
        //given
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.BACKEND_SERVICE).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);

        //when
        RuleResult result = rule.evaluate(component, NO_PARAMS);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Not applicable (not a self-contained system)");
        verifyNoInteractions(promTimeSeriesQueryRepository);
    }

    private SystemComponent scsComponent() {
        SystemComponent component = SystemComponent.builder().name("test").type(ComponentType.SELF_CONTAINED_SYSTEM).createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "id", 1L);
        return component;
    }

    private PromTimeSeries createSample(SystemComponent component, String name, String version) {
        return PromTimeSeries.builder()
                .prometheusQueryType(PromQueryType.JEAP_DEPENDENCY_VERSION)
                .queryTimestamp(ZonedDateTime.now())
                .systemComponentId(component.getId())
                .sample(new PromTimeSeriesSample(Map.of("name", name, "version", version), List.of("1")))
                .build();
    }
}
