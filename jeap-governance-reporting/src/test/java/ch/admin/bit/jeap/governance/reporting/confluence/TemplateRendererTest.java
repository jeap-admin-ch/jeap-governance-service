package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.reporting.confluence.model.ComponentScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemsScoreReportModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

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

}
