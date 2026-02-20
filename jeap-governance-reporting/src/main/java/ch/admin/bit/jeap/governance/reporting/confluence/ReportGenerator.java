package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.reporting.ReportingProperties;
import ch.admin.bit.jeap.governance.reporting.confluence.model.ComponentScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.ModelTransformer;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RulesReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemsScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRule;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingSystemScore;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportGenerator {

    private static final String SYSTEM_SCORES_PAGE_NAME = "System Scores";
    private static final String SYSTEM_SCORES_PAGE_SUFFIX = " (System scores)";
    private static final String COMPONENT_SCORES_PAGE_SUFFIX = " (Component scores)";
    private static final String RULES_PAGE_NAME = "Rules";

    private final ConfluenceAdapter confluenceAdapter;
    private final TemplateRenderer templateRenderer;
    private final ReportingProperties properties;
    private final ModelTransformer modelTransformer = new ModelTransformer(SYSTEM_SCORES_PAGE_SUFFIX, COMPONENT_SCORES_PAGE_SUFFIX);

    @Timed("jeap.governance.service.reporting.systems.generation")
    public void generateSystemsReport(List<ReportingSystemScore> systemScores, boolean withOrphanCleanup) {
        String rootPageId = confluenceAdapter.getPageByName(properties.getConfluenceRootPageName());
        List<SystemScoreReportModel> systemScoresSorted = transformAndSort(systemScores);
        SystemsScoreReportModel systemsScoreReportModel = SystemsScoreReportModel.builder()
                .title(SYSTEM_SCORES_PAGE_NAME + " overview")
                .systemScores(systemScoresSorted)
                .build();

        String content = templateRenderer.renderSystemsScorePage(systemsScoreReportModel);
        String systemsPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(rootPageId, SYSTEM_SCORES_PAGE_NAME, content);

        GeneratorContext context = new GeneratorContext(systemsPageId);
        systemsScoreReportModel.getSystemScores().forEach(system -> generateSystem(context, systemsPageId, system));
        log.info("Documentation generated, containing {} pages", context.getGeneratedPageIds().size());
        if (withOrphanCleanup) {
            int deletedPageCount = confluenceAdapter.deleteOrphanPages(context.getRootPageId(), context.getGeneratedPageIds());
            log.info("Orphan cleanup done, deleted {} pages", deletedPageCount);
        }
    }

    @Timed("jeap.governance.service.reporting.rules.generation")
    public void generateRulesReport(List<ReportingRule> reportingRules, boolean withOrphanCleanup) {
        String rootPageId = confluenceAdapter.getPageByName(properties.getConfluenceRootPageName());
        List<RuleReportModel> rulesSorted = transformAndSortRules(reportingRules);
        RulesReportModel rulesReportModel = RulesReportModel.builder()
                .title(RULES_PAGE_NAME)
                .rules(rulesSorted)
                .build();
        String rulesPageContent = templateRenderer.renderRulesPage(rulesReportModel);
        String rulesPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(rootPageId, RULES_PAGE_NAME, rulesPageContent);
        GeneratorContext context = new GeneratorContext(rulesPageId);

        rulesReportModel.getRules().forEach(rule -> generateRule(context, rulesPageId, rule));

        log.info("Documentation generated, containing {} pages", context.getGeneratedPageIds().size());
        if (withOrphanCleanup) {
            int deletedPageCount = confluenceAdapter.deleteOrphanPages(context.getRootPageId(), context.getGeneratedPageIds());
            log.info("Orphan cleanup done, deleted {} pages", deletedPageCount);
        }
    }

    private void generateRule(GeneratorContext context, String rulesPageId, RuleReportModel rule) {
        try {
            String rulePageContent = templateRenderer.renderRulePage(rule);
            String rulePageName = rule.getName();
            String rulePageId = confluenceAdapter.addOrUpdatePageUnderAncestor(rulesPageId, rulePageName, rulePageContent);
            context.addGeneratedPageIds(rulePageId);
        } catch (Exception e) {
            log.error("Error generating report for rule '{}': {}", rule.getName(), e.getMessage(), e);
        }
    }

    private List<SystemScoreReportModel> transformAndSort(Collection<ReportingSystemScore> systemScores) {
        List<SystemScoreReportModel> systemScoresSorted = new ArrayList<>(systemScores.stream()
                .map(modelTransformer::toConfluenceModel)
                .toList());
        // sort by score and then by name
        systemScoresSorted.sort(Comparator.comparing(SystemScoreReportModel::getScore).reversed().thenComparing(SystemScoreReportModel::getSystemName, String.CASE_INSENSITIVE_ORDER));
        return systemScoresSorted;
    }

    private void generateSystem(GeneratorContext context, String systemsPageId, SystemScoreReportModel system) {
        try {
            doGenerateSystem(context, systemsPageId, system);
        } catch (Exception e) {
            log.error("Error generating report for system '{}': {}", system.getSystemName(), e.getMessage(), e);
        }
    }

    private void doGenerateSystem(GeneratorContext context, String systemsPageId, SystemScoreReportModel system) {
        String systemPageContent = templateRenderer.renderSystemScorePage(system);
        String systemPageName = system.getSystemName() + SYSTEM_SCORES_PAGE_SUFFIX;
        String systemPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(systemsPageId, systemPageName, systemPageContent);

        context.addGeneratedPageIds(systemPageId);

        for (ComponentScoreReportModel componentModel : system.getComponentScores()) {
            try {
                doGenerateComponent(systemPageId, context, componentModel);
            } catch (Exception e) {
                log.error("Error generating report for component '{}': {}", componentModel.getComponentName(), e.getMessage(), e);
            }
        }
    }

    private void doGenerateComponent(String systemPageId, GeneratorContext context, ComponentScoreReportModel
            componentModel) {
        String componentPageContent = templateRenderer.renderComponentScorePage(componentModel);
        String componentPageName = componentModel.getComponentName() + COMPONENT_SCORES_PAGE_SUFFIX;
        String componentPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(systemPageId, componentPageName, componentPageContent);
        context.addGeneratedPageIds(componentPageId);
    }

    private List<RuleReportModel> transformAndSortRules(List<ReportingRule> reportingRules) {
        List<RuleReportModel> rulesSorted = new ArrayList<>(reportingRules.stream()
                .map(modelTransformer::toConfluenceModel)
                .toList());
        rulesSorted.sort(Comparator.comparing(RuleReportModel::getConformanceRate).reversed().thenComparing(RuleReportModel::getRuleId, String.CASE_INSENSITIVE_ORDER));
        return rulesSorted;
    }
}
