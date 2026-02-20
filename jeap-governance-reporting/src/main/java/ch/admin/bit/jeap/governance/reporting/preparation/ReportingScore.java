package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import lombok.Getter;

import java.time.LocalDate;

public class ReportingScore implements Comparable<ReportingScore>, TrendValueHolder {
    @Getter
    private final LocalDate day;
    @Getter
    private final int score;

    ReportingScore(SystemScore systemScore) {
        this.day = systemScore.getDay();
        this.score = systemScore.getScore();
    }

    ReportingScore(ComponentScore componentScore) {
        this.day = componentScore.getDay();
        this.score = componentScore.getScore();
    }

    public ReportingScore(LocalDate day, int score) {
        this.day = day;
        this.score = score;
    }

    @Override
    public int getValue() {
        return getScore();
    }

    @Override
    public int compareTo(ReportingScore other) {
        return this.getDay().compareTo(other.getDay());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReportingScore other && this.getDay().equals(other.getDay()) && this.getScore() == other.getScore();
    }

    @Override
    public int hashCode() {
        return this.getDay().hashCode() * 31 + Integer.hashCode(this.getScore());
    }
}
