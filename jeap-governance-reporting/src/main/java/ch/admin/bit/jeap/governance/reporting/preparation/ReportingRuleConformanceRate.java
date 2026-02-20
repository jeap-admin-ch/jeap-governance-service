package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class ReportingRuleConformanceRate implements Comparable<ReportingRuleConformanceRate>, TrendValueHolder {
    @Getter
    private final LocalDate day;
    @Getter
    private final int rate;

    ReportingRuleConformanceRate(RuleConformanceRate conformanceRate) {
        this.day = conformanceRate.getDay();
        this.rate = conformanceRate.getConformanceRate();
    }

    @Override
    public int getValue() {
        return getRate();
    }

    @Override
    public int compareTo(ReportingRuleConformanceRate other) {
        return this.getDay().compareTo(other.getDay());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReportingRuleConformanceRate other && this.getDay().equals(other.getDay()) && this.getRate() == other.getRate();
    }

    @Override
    public int hashCode() {
        return this.getDay().hashCode() * 31 + Integer.hashCode(this.getRate());
    }
}
