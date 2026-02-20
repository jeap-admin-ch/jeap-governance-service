package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.reporting.confluence.model.ComponentScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RulesReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemsScoreReportModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
class TemplateRenderer {

    private static final String MODEL_VARIABLE_NAME = "model";

    private final ITemplateEngine templateEngine;

    String renderSystemsScorePage(SystemsScoreReportModel model) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable(MODEL_VARIABLE_NAME, model);
        return templateEngine.process("systems", context).trim();
    }

    String renderSystemScorePage(SystemScoreReportModel model) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable(MODEL_VARIABLE_NAME, model);
        return templateEngine.process("system", context).trim();
    }

    String renderComponentScorePage(ComponentScoreReportModel model) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable(MODEL_VARIABLE_NAME, model);
        return templateEngine.process("component", context).trim();
    }

    public String renderRulesPage(RulesReportModel model) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable(MODEL_VARIABLE_NAME, model);
        return templateEngine.process("rules", context).trim();
    }

    public String renderRulePage(RuleReportModel model) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable(MODEL_VARIABLE_NAME, model);
        return templateEngine.process("rule", context).trim();
    }
}
