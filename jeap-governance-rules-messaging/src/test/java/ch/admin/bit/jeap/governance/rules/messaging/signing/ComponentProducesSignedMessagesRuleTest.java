package ch.admin.bit.jeap.governance.rules.messaging.signing;

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
class ComponentProducesSignedMessagesRuleTest {

    @Mock
    private PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @InjectMocks
    private ComponentProducesSignedMessagesRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-produces-signedmessages");
        assertThat(metadata.label()).isEqualTo("Component Produces Signed Messages");
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
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, 1L))
                .thenReturn(List.of(PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_MESSAGING_TOTAL)
                        .queryTimestamp(ZonedDateTime.now())
                        .sample(new PromTimeSeriesSample(Map.of("foo", "bar"), List.of("123")))
                        .build()));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("All messages are signed");
    }

    @Test
    void signatureNotEnforced() {
        //given
        SystemComponent component = mockComponent(2L);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, 2L))
                .thenReturn(List.of(PromTimeSeries.builder()
                        .prometheusQueryType(PromQueryType.JEAP_MESSAGING_TOTAL)
                        .queryTimestamp(ZonedDateTime.now())
                        .sample(new PromTimeSeriesSample(Map.of("type", "producer", "signed", "0"), List.of("foo")))
                        .build()));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Publisher sends unsigned messages");
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
