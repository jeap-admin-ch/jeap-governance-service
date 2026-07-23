package ch.admin.bit.jeap.governance.rules.messaging.contracts;

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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentDefinesMessagingContractsRuleTest {

    @Mock
    private PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @InjectMocks
    private ComponentDefinesMessagingContractsRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-defines-messagingcontracts");
        assertThat(metadata.label()).isEqualTo("Component Defines Messaging Contracts");
    }

    @Test
    void noMessaging() {
        //given
        SystemComponent component = mockComponent();

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No messaging library detected");
    }

    @Test
    void ok() {
        //given
        SystemComponent component = mockComponent(1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_CONTRACT, 1L))
                .thenReturn(mockPrometheusResponse("0", "0", "0"));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Contracts enabled");
    }

    @Test
    void noMasterContracts() {
        //given
        SystemComponent component = mockComponent(1L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_CONTRACT, 1L))
                .thenReturn(mockPrometheusResponse("1", "0", "0"));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Contracts from branched other than master");
    }

    @Test
    void consumeWithoutContract() {
        //given
        SystemComponent component = mockComponent(2L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_CONTRACT, 2L))
                .thenReturn(mockPrometheusResponse("0", "1", "0"));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Consume without contracts enabled");
    }

    @Test
    void publishWithoutContract() {
        //given
        SystemComponent component = mockComponent(3L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_CONTRACT, 3L))
                .thenReturn(mockPrometheusResponse("0", "0", "1"));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Publish without contracts enabled");
    }

    @Test
    void silentIgnoreWithoutContract() {
        //given
        SystemComponent component = mockComponent(4L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_CONTRACT, 4L))
                .thenReturn(mockPrometheusResponse("0", "0", "0", "1"));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Silently ignoring messages without contract enabled");
    }

    private List<PromTimeSeries> mockPrometheusResponse(String noMasterContractsValue, String consumeWithoutContractValue, String publishWithoutContractValue) {
        return mockPrometheusResponse(noMasterContractsValue, consumeWithoutContractValue, publishWithoutContractValue, "0");
    }

    private List<PromTimeSeries> mockPrometheusResponse(String noMasterContractsValue, String consumeWithoutContractValue, String publishWithoutContractValue, String silentIgnoreWithoutContractValue) {
        return List.of(
                mockPromTimeSeries("noMasterContracts", noMasterContractsValue),
                mockPromTimeSeries("consumeWithoutContract", consumeWithoutContractValue),
                mockPromTimeSeries("publishWithoutContract", publishWithoutContractValue),
                mockPromTimeSeries("silentIgnoreWithoutContract", silentIgnoreWithoutContractValue));
    }

    private PromTimeSeries mockPromTimeSeries(String key, String value) {
        return PromTimeSeries.builder()
                .prometheusQueryType(PromQueryType.JEAP_MESSAGING_CONTRACT)
                .queryTimestamp(ZonedDateTime.now())
                .sample(new PromTimeSeriesSample(Map.of("switch", key), List.of("123", value)))
                .build();
    }

    private SystemComponent mockComponent(long id) {
        var component = mock(SystemComponent.class);
        when(component.getId()).thenReturn(id);
        return component;
    }

    private SystemComponent mockComponent() {
        return mock(SystemComponent.class);
    }

}
