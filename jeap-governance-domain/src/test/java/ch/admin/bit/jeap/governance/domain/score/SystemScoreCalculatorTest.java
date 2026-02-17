package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemScoreCalculatorTest {

    private final SystemScoreCalculator calculator = new SystemScoreCalculator();
    private final LocalDate today = LocalDate.of(2026, 2, 17);

    @Test
    void noComponents_scoreIs100() {
        System system = system("test-system");

        SystemScore score = calculator.calculateSystemScore(system, today, List.of());

        assertThat(score.getScore()).isEqualTo(100);
        assertThat(score.getSystem()).isEqualTo(system);
        assertThat(score.getDay()).isEqualTo(today);
    }

    @Test
    void singleComponent_scoreEqualsComponentScore() {
        System system = system("test-system", "service-a");
        var componentScores = List.of(
                componentScore(system.getSystemComponents().getFirst(), 100)
        );

        SystemScore score = calculator.calculateSystemScore(system, today, componentScores);

        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void multipleComponents_allFullScore() {
        System system = system("test-system", "service-a", "service-b");
        var componentScores = List.of(
                componentScore(system.getSystemComponents().get(0), 100),
                componentScore(system.getSystemComponents().get(1), 100)
        );

        SystemScore score = calculator.calculateSystemScore(system, today, componentScores);

        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void multipleComponents_mixedScores_returnsAverage() {
        System system = system("test-system", "service-a", "service-b");
        var componentScores = List.of(
                componentScore(system.getSystemComponents().get(0), 100),
                componentScore(system.getSystemComponents().get(1), 50)
        );

        SystemScore score = calculator.calculateSystemScore(system, today, componentScores);

        // average = (100+50)/2 = 75
        assertThat(score.getScore()).isEqualTo(75);
    }

    @Test
    void singleComponent_lowScore() {
        System system = system("test-system", "service-a");
        var componentScores = List.of(
                componentScore(system.getSystemComponents().getFirst(), 30)
        );

        SystemScore score = calculator.calculateSystemScore(system, today, componentScores);

        assertThat(score.getScore()).isEqualTo(30);
    }

    @Test
    void multipleComponents_allZero() {
        System system = system("test-system", "service-a", "service-b");
        var componentScores = List.of(
                componentScore(system.getSystemComponents().get(0), 0),
                componentScore(system.getSystemComponents().get(1), 0)
        );

        SystemScore score = calculator.calculateSystemScore(system, today, componentScores);

        assertThat(score.getScore()).isEqualTo(0);
    }

    private System system(String name, String... componentNames) {
        List<SystemComponent> components = java.util.Arrays.stream(componentNames)
                .map(n -> SystemComponent.builder()
                        .name(n)
                        .state(State.OK)
                        .type(ComponentType.BACKEND_SERVICE)
                        .build())
                .toList();
        return System.builder()
                .name(name)
                .aliases(Set.of())
                .systemComponents(components)
                .state(State.OK)
                .build();
    }

    private ComponentScore componentScore(SystemComponent component, int score) {
        return ComponentScore.builder()
                .systemComponent(component)
                .day(today)
                .score(score)
                .build();
    }
}
