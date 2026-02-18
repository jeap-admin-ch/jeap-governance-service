package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.System;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Calculates a system's compliance score as the average of its component scores.
 */
@Component
class SystemScoreCalculator {

    public SystemScore calculateSystemScore(System system, LocalDate day, List<ComponentScore> componentScores) {
        if (componentScores.isEmpty()) {
            return SystemScore.builder()
                    .system(system)
                    .day(day)
                    .score(100) // If there are no components, we can consider the system as fully compliant
                    .build();
        }

        return systemScore(system, day, componentScores);
    }

    private static SystemScore systemScore(System system, LocalDate day, List<ComponentScore> componentScores) {
        double totalScore = componentScores.stream()
                .mapToDouble(ComponentScore::getScore)
                .sum();
        double averageScore = Math.min(100, totalScore / componentScores.size());
        return SystemScore.builder()
                .system(system)
                .day(day)
                .score((int) averageScore)
                .build();
    }
}
