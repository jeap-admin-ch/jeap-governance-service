package ch.admin.bit.jeap.governance.reportdata;

import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ch.admin.bit.jeap.governance.reportdata.TrendIndicatorUtility.calculateTrendIndicator;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportingComponentScore {
    @Getter
    private final String componentName;

    private final List<ReportingRuleState> ruleStates;

    private final List<ReportingScore> scores = new ArrayList<>();

    void addScore(ComponentScore componentScore) {
        scores.add(new ReportingScore(componentScore));
        Collections.sort(scores);
    }

    public List<ReportingRuleState> getRuleStates() {
        return new ArrayList<>(ruleStates);
    }

    public List<ReportingScore> getScores() {
        return new ArrayList<>(scores);
    }

    public TrendIndicator getScoringTrend() {
        return calculateTrendIndicator(scores);
    }
}
