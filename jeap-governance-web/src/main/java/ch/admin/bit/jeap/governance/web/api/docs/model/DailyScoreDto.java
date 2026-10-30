package ch.admin.bit.jeap.governance.web.api.docs.model;

import java.time.LocalDate;

public record DailyScoreDto(LocalDate day, int score) {
}
