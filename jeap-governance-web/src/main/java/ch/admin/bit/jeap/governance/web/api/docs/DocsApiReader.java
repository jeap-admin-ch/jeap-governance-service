package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.db.tx.RetryOnDatabaseFailover;
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.governance.domain.rule.RuleDescription;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.reportdata.LastScoringRun;
import ch.admin.bit.jeap.governance.reportdata.ReportingRule;
import ch.admin.bit.jeap.governance.reportdata.ReportingRulesPreparation;
import ch.admin.bit.jeap.governance.reportdata.ReportingSystemScore;
import ch.admin.bit.jeap.governance.reportdata.ReportingSystemsPreparation;
import ch.admin.bit.jeap.governance.reportdata.TrendPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads the report data the docs API delivers, from the same preparation the Confluence reporting uses, so that
 * both show the same values.
 * <p>
 * Each read is one read-only transaction on the read replica, retried once after a database failover. The
 * annotations sit here, on the methods that open the transaction, since the retry only repeats a transaction it
 * started itself.
 */
@Component
@RequiredArgsConstructor
class DocsApiReader {

    private final ReportingSystemsPreparation systemsPreparation;
    private final ReportingRulesPreparation rulesPreparation;
    private final RuleRepository ruleRepository;
    private final LastScoringRun lastScoringRun;
    private final TrendPeriod trendPeriod;

    @TransactionalReadReplica
    @RetryOnDatabaseFailover
    public SystemsData readSystems() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<ReportingSystemScore> systems = systemsPreparation.prepareAllSystemsScores(trendPeriod.fromDay(today), today);
        return new SystemsData(lastScoringRun.completedAt().orElse(null), trendPeriod.days(), systems);
    }

    @TransactionalReadReplica
    @RetryOnDatabaseFailover
    public RulesData readRules() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Map<String, ReportingRule> preparedRules = rulesPreparation.prepareAllRules(trendPeriod.fromDay(today), today).stream()
                .collect(Collectors.toMap(ReportingRule::getRuleId, Function.identity()));
        return new RulesData(lastScoringRun.completedAt().orElse(null), today, ruleRepository.getKnownRules(),
                ruleRepository.getActiveRuleShares(), preparedRules);
    }

    record SystemsData(OffsetDateTime scoredAt, int trendPeriodDays, List<ReportingSystemScore> systems) {
    }

    /**
     * @param preparedRules the prepared data of the active rules that have been scored, by rule ID
     */
    record RulesData(OffsetDateTime scoredAt, LocalDate today, List<RuleDescription> knownRules,
                     Map<RuleId, BigDecimal> shares, Map<String, ReportingRule> preparedRules) {
    }
}
