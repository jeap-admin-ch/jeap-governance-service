package ch.admin.bit.jeap.governance.reporting.preparation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendIndicatorUtilityTest {

    @Test
    void testScoringTrend_up() {
        List<TestTrendValueHolder> scores = List.of(
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(90)
        );
        assertEquals(TrendIndicator.UP, TrendIndicatorUtility.calculateTrendIndicator(scores));
    }

    @Test
    void testScoringTrend_unknown_whenOneEntry() {
        List<TestTrendValueHolder> scores = List.of(
                new TestTrendValueHolder(90)
        );
        assertEquals(TrendIndicator.UNKNOWN, TrendIndicatorUtility.calculateTrendIndicator(scores));
    }

    @Test
    void testScoringTrend_stable() {
        List<TestTrendValueHolder> scores = List.of(
                new TestTrendValueHolder(90),
                new TestTrendValueHolder(90)
        );
        assertEquals(TrendIndicator.STABLE, TrendIndicatorUtility.calculateTrendIndicator(scores));
    }

    @Test
    void testScoringTrend_down() {
        List<TestTrendValueHolder> scores = List.of(
                new TestTrendValueHolder(90),
                new TestTrendValueHolder(80)
        );
        assertEquals(TrendIndicator.DOWN, TrendIndicatorUtility.calculateTrendIndicator(scores));
    }

    @Test
    void testScoringTrend_up_moreEntries() {
        List<TestTrendValueHolder> scores = List.of(
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(70),
                new TestTrendValueHolder(90),
                new TestTrendValueHolder(90)
        );
        assertEquals(TrendIndicator.UP, TrendIndicatorUtility.calculateTrendIndicator(scores));
    }

    @Test
    void testScoringTrend_stable_moreEntries() {
        List<TestTrendValueHolder> scores = List.of(
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(79),
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(90),
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(90),
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(80),
                new TestTrendValueHolder(80)
        );
        assertEquals(TrendIndicator.STABLE, TrendIndicatorUtility.calculateTrendIndicator(scores));
    }


    @Test
    void testScoringTrend_noData() {
        assertEquals(TrendIndicator.NO_DATA, TrendIndicatorUtility.calculateTrendIndicator(List.of()));
    }

    private static class TestTrendValueHolder implements TrendValueHolder {
        private final int value;

        public TestTrendValueHolder(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return value;
        }
    }

}
