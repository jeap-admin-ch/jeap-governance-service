package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static ch.admin.bit.jeap.governance.reporting.preparation.TrendIndicatorUtility.calculateTrendIndicator;

@RequiredArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ReportingSystemScore {
    @Getter
    private final Long systemId;
    @Getter
    private final String systemName;
    private final Map<Long, ReportingComponentScore> componentScoreContainers;
    private final List<ReportingScore> scores = new ArrayList<>();

    void addScore(SystemScore systemScore) {
        scores.add(new ReportingScore(systemScore));
        Collections.sort(scores);
    }

    /**
     * @return scores sorted by day ascending (oldest first)
     */
    public List<ReportingScore> getScores() {
        return new ArrayList<>(scores);
    }

    public TrendIndicator getScoringTrend() {
        return calculateTrendIndicator(scores);
    }

    public int getLatestScore() {
        return scores.isEmpty() ? 0 : scores.getLast().getScore();
    }

    public List<ReportingComponentScore> getComponentScores() {
        List<ReportingComponentScore> result = new ArrayList<>(componentScoreContainers.values());
        Collections.sort(result, Comparator.comparing(ReportingComponentScore::getComponentName));
        return result;
    }
}
