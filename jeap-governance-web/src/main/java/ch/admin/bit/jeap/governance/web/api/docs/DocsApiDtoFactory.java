package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleComponentExemption;
import ch.admin.bit.jeap.governance.domain.rule.RuleDescription;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.reportdata.ReportingComponentScore;
import ch.admin.bit.jeap.governance.reportdata.ReportingRule;
import ch.admin.bit.jeap.governance.reportdata.ReportingRuleGracePeriodComponent;
import ch.admin.bit.jeap.governance.reportdata.ReportingRuleNonCompliantComponent;
import ch.admin.bit.jeap.governance.reportdata.ReportingRuleState;
import ch.admin.bit.jeap.governance.reportdata.ReportingRuleSystemConformanceRate;
import ch.admin.bit.jeap.governance.reportdata.ReportingScore;
import ch.admin.bit.jeap.governance.reportdata.ReportingSystemScore;
import ch.admin.bit.jeap.governance.reportdata.TrendIndicator;
import ch.admin.bit.jeap.governance.web.api.docs.DocsApiReader.RulesData;
import ch.admin.bit.jeap.governance.web.api.docs.DocsApiReader.SystemsData;
import ch.admin.bit.jeap.governance.web.api.docs.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns the report data into the payloads of the docs API.
 * <p>
 * Every list is sorted and every map written as a sorted list, so that the same data always gives the same bytes
 * and with them the same entity tag. An index computes the tag of each resource it lists from the very payload that
 * resource answers with, which makes the two byte-identical.
 */
@Component
class DocsApiDtoFactory {

    /**
     * Neither the ID nor the component name of an exemption is validated, so either may be missing.
     */
    private static final Comparator<String> NULLS_LAST = Comparator.nullsLast(Comparator.naturalOrder());

    private final DocsApiEtagSupport etagSupport;
    private final ZoneId zone;

    @Autowired
    DocsApiDtoFactory(DocsApiEtagSupport etagSupport) {
        this(etagSupport, ZoneId.systemDefault());
    }

    DocsApiDtoFactory(DocsApiEtagSupport etagSupport, ZoneId zone) {
        this.etagSupport = etagSupport;
        this.zone = zone;
    }

    SystemIndexDto systemIndex(SystemsData data) {
        List<SystemIndexEntryDto> entries = sortedSystems(data).stream()
                .map(system -> {
                    SystemDto dto = system(system);
                    return new SystemIndexEntryDto(dto.name(), dto.score(), dto.trend(), etagSupport.entityTagOf(dto),
                            DocsApiPaths.systemPath(dto.name()));
                })
                .toList();
        return new SystemIndexDto(data.scoredAt(), data.trendPeriodDays(), entries);
    }

    Optional<SystemDto> system(SystemsData data, String systemName) {
        return data.systems().stream()
                .filter(system -> system.getSystemName().equals(systemName))
                .findFirst()
                .map(this::system);
    }

    RuleIndexDto ruleIndex(RulesData data) {
        List<RuleIndexEntryDto> entries = data.knownRules().stream()
                .map(description -> {
                    RuleDto dto = rule(data, description);
                    return new RuleIndexEntryDto(dto.ruleId(), dto.name(), dto.active(), dto.weight(), dto.share(),
                            dto.conformanceRate(), dto.trend(), dto.active() ? exemptionsInForce(dto) : null,
                            etagSupport.entityTagOf(dto), DocsApiPaths.rulePath(dto.ruleId()));
                })
                .toList();
        return new RuleIndexDto(data.scoredAt(), entries);
    }

    Optional<RuleDto> rule(RulesData data, String ruleId) {
        return data.knownRules().stream()
                .filter(description -> description.ruleId().id().equals(ruleId))
                .findFirst()
                .map(description -> rule(data, description));
    }

    private static List<ReportingSystemScore> sortedSystems(SystemsData data) {
        return data.systems().stream()
                .sorted(Comparator.comparing(ReportingSystemScore::getSystemName))
                .toList();
    }

    private SystemDto system(ReportingSystemScore system) {
        List<ComponentDto> components = system.getComponentScores().stream()
                .sorted(Comparator.comparing(ReportingComponentScore::getComponentName))
                .map(this::component)
                .toList();
        return new SystemDto(system.getSystemName(), latestScore(system.getScores()), trend(system.getScoringTrend()),
                history(system.getScores()), components);
    }

    private ComponentDto component(ReportingComponentScore component) {
        List<RuleStateDto> ruleStates = component.getRuleStates().stream()
                .sorted(Comparator.comparing(ReportingRuleState::ruleId))
                .map(this::ruleState)
                .toList();
        return new ComponentDto(component.getComponentName(), latestScore(component.getScores()),
                trend(component.getScoringTrend()), history(component.getScores()), ruleStates);
    }

    private RuleStateDto ruleState(ReportingRuleState ruleState) {
        Comment comment = Comment.of(ruleState.stateComment());
        return new RuleStateDto(ruleState.ruleId(), ruleState.label(), stateValue(ruleState.state()),
                comment.summary(), comment.details(), offset(ruleState.modifiedAt()));
    }

    private RuleDto rule(RulesData data, RuleDescription description) {
        String ruleId = description.ruleId().id();
        if (!description.active()) {
            return new RuleDto(ruleId, description.label(), false, description.explanation(), null, null, null,
                    false, List.of(), List.of(), null, null, List.of(), List.of(), List.of());
        }
        Optional<ReportingRule> prepared = Optional.ofNullable(data.preparedRules().get(ruleId));
        boolean gracePeriodConfigured = description.violationDelay() != null && description.violationDelay().isPositive();
        return new RuleDto(ruleId, description.label(), true, description.explanation(),
                description.weight(),
                data.shares().get(description.ruleId()),
                description.violationDelay() == null ? null : description.violationDelay().toString(),
                gracePeriodConfigured,
                parameters(description.parameters()),
                exemptions(description.exemptions(), data.today()),
                prepared.filter(rule -> !rule.getConformanceRates().isEmpty())
                        .map(ReportingRule::getLatestConformanceRate)
                        .orElse(null),
                prepared.map(rule -> trend(rule.getConformanceRateTrend())).orElse(Trend.NO_DATA),
                prepared.map(this::systemConformances).orElse(List.of()),
                prepared.map(this::nonCompliantComponents).orElse(List.of()),
                gracePeriodConfigured ? prepared.map(this::gracePeriodComponents).orElse(List.of()) : List.of());
    }

    private List<SystemConformanceDto> systemConformances(ReportingRule rule) {
        return rule.getSystemConformanceRates().stream()
                .sorted(Comparator.comparing(ReportingRuleSystemConformanceRate::getSystemName))
                .map(rate -> new SystemConformanceDto(rate.getSystemName(), rate.getLatestConformanceRate()))
                .toList();
    }

    private List<NonCompliantComponentDto> nonCompliantComponents(ReportingRule rule) {
        return rule.getNonCompliantComponents().stream()
                .sorted(Comparator.comparing(ReportingRuleNonCompliantComponent::getSystemName)
                        .thenComparing(ReportingRuleNonCompliantComponent::getComponentName))
                .map(component -> new NonCompliantComponentDto(component.getSystemName(), component.getComponentName(),
                        day(component.getNonComplianceSince())))
                .toList();
    }

    private List<GracePeriodComponentDto> gracePeriodComponents(ReportingRule rule) {
        // Kept in the order of the preparation: by the end of the grace period, then system and component
        return rule.getGracePeriodComponents().stream()
                .map(this::gracePeriodComponent)
                .toList();
    }

    private GracePeriodComponentDto gracePeriodComponent(ReportingRuleGracePeriodComponent component) {
        Comment comment = Comment.of(component.getStateComment());
        return new GracePeriodComponentDto(component.getSystemName(), component.getComponentName(),
                comment.summary(), comment.details(),
                offset(component.getViolationDetectedAt()), offset(component.getGracePeriodEndsAt()));
    }

    private static List<ParameterDto> parameters(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ParameterDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<ExemptionDto> exemptions(List<RuleComponentExemption> exemptions, LocalDate today) {
        return exemptions.stream()
                .sorted(Comparator.comparing(RuleComponentExemption::componentName, NULLS_LAST)
                        .thenComparing(RuleComponentExemption::id, NULLS_LAST))
                .map(exemption -> new ExemptionDto(exemption.id(), exemption.componentName(), exemption.reason(),
                        exemption.until(), effect(exemption.activationState(today)), parameters(exemption.parameters())))
                .toList();
    }

    private static int exemptionsInForce(RuleDto rule) {
        return (int) rule.exemptions().stream()
                .filter(exemption -> exemption.effect() != ExemptionEffect.EXPIRED)
                .count();
    }

    private static ExemptionEffect effect(RuleActivationState activationState) {
        return switch (activationState) {
            case EXEMPTED -> ExemptionEffect.DISABLED;
            case EXEMPTED_UNTIL -> ExemptionEffect.PAUSED;
            case ACTIVE -> ExemptionEffect.EXPIRED;
        };
    }

    private static Integer latestScore(List<ReportingScore> scores) {
        return scores.isEmpty() ? null : scores.getLast().getScore();
    }

    private static List<DailyScoreDto> history(List<ReportingScore> scores) {
        return scores.stream()
                .map(score -> new DailyScoreDto(score.getDay(), score.getScore()))
                .toList();
    }

    private static Trend trend(TrendIndicator trendIndicator) {
        return Trend.valueOf(trendIndicator.name());
    }

    private static RuleStateValue stateValue(State state) {
        return state == null ? RuleStateValue.UNKNOWN : RuleStateValue.valueOf(state.name());
    }

    private OffsetDateTime offset(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.withZoneSameInstant(zone).toOffsetDateTime();
    }

    private LocalDate day(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.withZoneSameInstant(zone).toLocalDate();
    }

    /**
     * A rule comment is plain text: its first line is the summary, every further non-blank line one item.
     */
    record Comment(String summary, List<String> details) {

        static Comment of(String stateComment) {
            if (stateComment == null || stateComment.isBlank()) {
                return new Comment(null, List.of());
            }
            List<String> lines = stateComment.strip().lines().toList();
            List<String> details = lines.stream()
                    .skip(1)
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .toList();
            return new Comment(lines.getFirst().strip(), details);
        }
    }
}
