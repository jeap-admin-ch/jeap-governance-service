package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.reporting.confluence.model.ComponentScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleReportComponentModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleStateReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.State;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemsScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.Trend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateRendererTest {

    private static final String RENDERED_CONTENT = "renderedContent";

    @Mock
    private ITemplateEngine templateEngine;

    @InjectMocks
    private TemplateRenderer templateRenderer;

    @Test
    void renderSystemsScorePage() {
        SystemsScoreReportModel model = mock(SystemsScoreReportModel.class);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("systems"), contextCaptor.capture())).thenReturn(RENDERED_CONTENT);

        String result = templateRenderer.renderSystemsScorePage(model);

        assertEquals(RENDERED_CONTENT, result);
        Context context = contextCaptor.getValue();
        assertEquals(model, context.getVariable("model"));
    }

    @Test
    void renderSystemScorePage() {
        SystemScoreReportModel model = mock(SystemScoreReportModel.class);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("system"), contextCaptor.capture())).thenReturn(RENDERED_CONTENT);

        String result = templateRenderer.renderSystemScorePage(model);

        assertEquals(RENDERED_CONTENT, result);
        Context context = contextCaptor.getValue();
        assertEquals(model, context.getVariable("model"));
    }

    @Test
    void renderComponentScorePage() {
        ComponentScoreReportModel model = mock(ComponentScoreReportModel.class);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("component"), contextCaptor.capture())).thenReturn(RENDERED_CONTENT);

        String result = templateRenderer.renderComponentScorePage(model);

        assertEquals(RENDERED_CONTENT, result);
        Context context = contextCaptor.getValue();
        assertEquals(model, context.getVariable("model"));
    }

    @Test
    void renderComponentScorePage_formatsMultilineRuleCommentAsBulletList() {
        var renderer = new TemplateRenderer(templateEngine());
        var ruleState = RuleStateReportModel.builder()
                .ruleId("message-contract-rule")
                .label("Message contract rule")
                .state(State.FAIL)
                .stateComment("""
                        Outdated message contracts:
                        FirstEvent uses 1.0.0, latest is 2.0.0

                        SecondEvent uses 1.1.0, latest is 2.0.0""")
                .build();
        var model = ComponentScoreReportModel.builder()
                .componentName("test-service")
                .pageSuffix("")
                .score(50)
                .trend(Trend.EVEN)
                .scores(List.of())
                .ruleStates(List.of(ruleState))
                .build();

        String result = renderer.renderComponentScorePage(model);

        assertThat(result)
                .contains("<div>Outdated message contracts:</div>")
                .contains("<ul>")
                .contains("<li>FirstEvent uses 1.0.0, latest is 2.0.0</li>")
                .contains("<li>SecondEvent uses 1.1.0, latest is 2.0.0</li>");
    }

    @Test
    void renderRulePage_showsComponentsInViolationGracePeriod() {
        var renderer = new TemplateRenderer(templateEngine());
        var detectedAt = ZonedDateTime.parse("2026-08-21T10:30:00+02:00[Europe/Zurich]");
        var component = RuleReportComponentModel.builder()
                .id(2L)
                .name("Component B")
                .pageSuffix(" (Component scores)")
                .systemName("System A")
                .systemPageSuffix(" (System scores)")
                .violationDetectedAt(detectedAt)
                .gracePeriodEndsAt(detectedAt.plusDays(7))
                .build();
        var model = RuleReportModel.builder()
                .ruleId("rule-1")
                .name("Rule One")
                .documentationLink("https://example.org/rule-1")
                .conformanceRate(100)
                .trend(Trend.EVEN)
                .systems(List.of())
                .violationGracePeriodConfigured(true)
                .gracePeriodComponents(List.of(component))
                .nonCompliantComponents(List.of())
                .build();

        String result = renderer.renderRulePage(model);

        assertThat(result)
                .contains("<h2>Components in Violation Grace Period</h2>")
                .contains("System A")
                .contains("Component B")
                .contains("21.08.2026 10:30")
                .contains("28.08.2026 10:30");
    }

    @Test
    void renderRulePage_showsNeutralMessageWhenNoComponentIsInViolationGracePeriod() {
        var renderer = new TemplateRenderer(templateEngine());
        var model = RuleReportModel.builder()
                .ruleId("rule-1")
                .name("Rule One")
                .documentationLink("https://example.org/rule-1")
                .conformanceRate(100)
                .trend(Trend.EVEN)
                .systems(List.of())
                .violationGracePeriodConfigured(true)
                .gracePeriodComponents(List.of())
                .nonCompliantComponents(List.of())
                .build();

        String result = renderer.renderRulePage(model);

        assertThat(result)
                .contains("<h2>Components in Violation Grace Period</h2>")
                .contains("No components are currently in the violation grace period.")
                .doesNotContain("<th>Violation Detected</th>");
    }

    private static SpringTemplateEngine templateEngine() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/confluence/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        resolver.setTemplateMode(TemplateMode.HTML);
        var templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

}
