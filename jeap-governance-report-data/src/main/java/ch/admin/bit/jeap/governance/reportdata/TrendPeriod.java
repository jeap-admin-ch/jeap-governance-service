package ch.admin.bit.jeap.governance.reportdata;

import java.time.LocalDate;

/**
 * The period the trends are calculated over, and the length of the score and conformance histories.
 */
public record TrendPeriod(int days) {

    public TrendPeriod {
        if (days < 1) {
            throw new IllegalArgumentException("The trend period must be at least one day, but was " + days);
        }
    }

    /**
     * @return the first day of the period that ends on the given day
     */
    public LocalDate fromDay(LocalDate untilDay) {
        return untilDay.minusDays(days);
    }
}
