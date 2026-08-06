package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleStateRepository;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRateRepository;
import ch.admin.bit.jeap.governance.domain.score.ComponentScoreRepository;
import ch.admin.bit.jeap.governance.domain.score.SystemScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class ReportingDataAccessTest {

    @Mock
    private SystemScoreRepository systemScoreRepository;
    @Mock
    private ComponentScoreRepository componentScoreRepository;
    @Mock
    private RuleRepository ruleRepository;
    @Mock
    private RuleStateRepository ruleStateRepository;
    @Mock
    private RuleConformanceRateRepository ruleConformanceRateRepository;
    @Mock
    private SystemRuleConformanceRateRepository systemRuleConformanceRateRepository;
    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemComponentRepository systemComponentRepository;

    @InjectMocks
    private ReportingDataAccess dataAccess;

    @Test
    void findIgnoredComponentIds_returnsPersistedGatewayIdsOnly() {
        SystemComponent backend = component("backend", ComponentType.BACKEND_SERVICE, 1L);
        SystemComponent gateway = component("gateway", ComponentType.GATEWAY, 2L);
        SystemComponent transientGateway = component("transient-gateway", ComponentType.GATEWAY, null);
        System system = System.builder()
                .name("system")
                .systemComponents(List.of(backend, gateway, transientGateway))
                .build();
        when(systemRepository.findAll()).thenReturn(List.of(system));

        assertThat(dataAccess.findIgnoredComponentIds()).containsExactly(2L);
    }

    private static SystemComponent component(String name, ComponentType type, Long id) {
        SystemComponent component = SystemComponent.builder().name(name).type(type).build();
        setField(component, "id", id);
        return component;
    }
}
