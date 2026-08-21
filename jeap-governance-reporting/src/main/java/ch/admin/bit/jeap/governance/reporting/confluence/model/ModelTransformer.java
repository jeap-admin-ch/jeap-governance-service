package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.reporting.preparation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ModelTransformer {

    private final String systemPageSuffix;
    private final String componentPageSuffix;
    private final ZoneId reportingZone;

    public ModelTransformer(String systemPageSuffix, String componentPageSuffix) {
        this(systemPageSuffix, componentPageSuffix, ZoneId.systemDefault());
    }

    ModelTransformer(String systemPageSuffix, String componentPageSuffix, ZoneId reportingZone) {
        this.systemPageSuffix = systemPageSuffix;
        this.componentPageSuffix = componentPageSuffix;
        this.reportingZone = reportingZone;
    }

    public SystemScoreReportModel toConfluenceModel(ReportingSystemScore reportingSystemScore) {
        return SystemScoreReportModel.builder()
                .systemName(reportingSystemScore.getSystemName())
                .pageSuffix(systemPageSuffix)
                .score(reportingSystemScore.getLatestScore())
                .scores(reportingSystemScore.getScores())
                .trend(toConfluenceTrend(reportingSystemScore.getScoringTrend()))
                .componentScores(componentsToConfluenceModel(reportingSystemScore.getComponentScores()))
                .build();
    }

    public RuleReportModel toConfluenceModel(ReportingRule reportingRule) {
        return RuleReportModel.builder()
                .name(reportingRule.getRuleName())
                .ruleId(reportingRule.getRuleId())
                .documentationLink(reportingRule.getDocumentationLink())
                .conformanceRate(reportingRule.getLatestConformanceRate())
                .trend(toConfluenceTrend(reportingRule.getConformanceRateTrend()))
                .systems(ruleSystemToConfluenceModel(reportingRule.getSystemConformanceRates()))
                .violationGracePeriodConfigured(reportingRule.hasViolationGracePeriod())
                .gracePeriodComponents(gracePeriodComponentsToConfluenceModel(reportingRule.getGracePeriodComponents()))
                .nonCompliantComponents(ruleComponentsToConfluenceModel(reportingRule.getNonCompliantComponents()))
                .build();
    }

    private List<ComponentScoreReportModel> componentsToConfluenceModel(List<ReportingComponentScore> componentScoreContainers) {
        List<ComponentScoreReportModel> result = new ArrayList<>(componentScoreContainers.stream()
                .map(this::toConfluenceModel)
                .toList());
        result.sort(Comparator.comparing(ComponentScoreReportModel::getScore).reversed().thenComparing(ComponentScoreReportModel::getComponentName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private ComponentScoreReportModel toConfluenceModel(ReportingComponentScore componentScoreContainer) {
        return ComponentScoreReportModel.builder()
                .componentName(componentScoreContainer.getComponentName())
                .pageSuffix(componentPageSuffix)
                .score(componentScoreContainer.getLatestScore())
                .scores(componentScoreContainer.getScores())
                .trend(toConfluenceTrend(componentScoreContainer.getScoringTrend()))
                .ruleStates(statesToConfluenceModel(componentScoreContainer.getRuleStates()))
                .build();
    }

    private List<RuleStateReportModel> statesToConfluenceModel(List<ReportingRuleState> ruleStates) {
        return ruleStates.stream()
                .map(this::toConfluenceModel)
                .toList();
    }

    private RuleStateReportModel toConfluenceModel(ReportingRuleState ruleState) {
        return RuleStateReportModel.builder()
                .ruleId(ruleState.ruleId())
                .label(ruleState.label())
                .documentationLink(ruleState.documentationLink())
                .state(toConfluenceState(ruleState.state()))
                .stateComment(ruleState.stateComment())
                .modifiedAt(ruleState.modifiedAt())
                .build();
    }

    private List<RuleReportSystemModel> ruleSystemToConfluenceModel(List<ReportingRuleSystemConformanceRate> conformanceRates) {
        List<RuleReportSystemModel> result = new ArrayList<>(conformanceRates.stream()
                .map(this::toConfluenceModel)
                .toList());
        result.sort(Comparator.comparing(RuleReportSystemModel::getConformanceRate).reversed().thenComparing(RuleReportSystemModel::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private RuleReportSystemModel toConfluenceModel(ReportingRuleSystemConformanceRate conformanceRate) {
        return RuleReportSystemModel.builder()
                .id(conformanceRate.getSystemId())
                .name(conformanceRate.getSystemName())
                .conformanceRate(conformanceRate.getLatestConformanceRate())
                .pageSuffix(systemPageSuffix)
                .build();
    }


    private List<RuleReportComponentModel> ruleComponentsToConfluenceModel(List<ReportingRuleNonCompliantComponent> nonCompliantComponents) {
        return nonCompliantComponents.stream()
                .map(this::toConfluenceModel)
                .toList();
    }

    private RuleReportComponentModel toConfluenceModel(ReportingRuleNonCompliantComponent nonCompliantComponent) {
        return RuleReportComponentModel.builder()
                .id(nonCompliantComponent.getComponentId())
                .name(nonCompliantComponent.getComponentName())
                .pageSuffix(componentPageSuffix)
                .systemName(nonCompliantComponent.getSystemName())
                .systemPageSuffix(systemPageSuffix)
                .nonCompliantSince(nonCompliantComponent.getNonComplianceSince())
                .build();
    }

    private List<RuleReportComponentModel> gracePeriodComponentsToConfluenceModel(
            List<ReportingRuleGracePeriodComponent> gracePeriodComponents) {
        return gracePeriodComponents.stream()
                .map(this::toConfluenceModel)
                .toList();
    }

    private RuleReportComponentModel toConfluenceModel(ReportingRuleGracePeriodComponent gracePeriodComponent) {
        return RuleReportComponentModel.builder()
                .id(gracePeriodComponent.getComponentId())
                .name(gracePeriodComponent.getComponentName())
                .pageSuffix(componentPageSuffix)
                .systemName(gracePeriodComponent.getSystemName())
                .systemPageSuffix(systemPageSuffix)
                .stateComment(gracePeriodComponent.getStateComment())
                .violationDetectedAt(gracePeriodComponent.getViolationDetectedAt().withZoneSameInstant(reportingZone))
                .gracePeriodEndsAt(gracePeriodComponent.getGracePeriodEndsAt().withZoneSameInstant(reportingZone))
                .build();
    }

    Trend toConfluenceTrend(TrendIndicator trendIndicator) {
        return switch (trendIndicator) {
            case UP -> Trend.UP;
            case DOWN -> Trend.DOWN;
            case STABLE -> Trend.EVEN;
            case UNKNOWN -> Trend.UNKNOWN;
            case NO_DATA -> Trend.NO_DATA;
        };
    }

    State toConfluenceState(ch.admin.bit.jeap.governance.domain.rule.State state) {
        return switch (state) {
            case OK -> State.OK;
            case PAUSED -> State.PAUSED;
            case FAIL -> State.FAIL;
            case DISABLED -> State.DISABLED;
        };
    }
}
