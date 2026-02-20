package ch.admin.bit.jeap.governance.reporting.preparation;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class TrendIndicatorUtility {

    /**
     * Calculates a trend indicator from an ordered list of data points using linear regression
     * (least-squares method). This approach considers all data points.
     *
     * @param trendValueHolders an ordered list of data points, typically sorted by time ascending;
     *                          must not be {@code null}
     */
    public static TrendIndicator calculateTrendIndicator(List<? extends TrendValueHolder> trendValueHolders) {
        if (trendValueHolders.isEmpty()) {
            return TrendIndicator.NO_DATA;
        }
        if (trendValueHolders.size() == 1) {
            return TrendIndicator.UNKNOWN;
        }

        int size = trendValueHolders.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < size; i++) {
            double x = i;
            double y = trendValueHolders.get(i).getValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Least-squares slope: m = (size·ΣXY - ΣX·ΣY) / (size·ΣX² - (ΣX)²)
        double denominator = size * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return TrendIndicator.STABLE;
        }
        double slope = (size * sumXY - sumX * sumY) / denominator;

        // Use a threshold to avoid noise being classified as UP/DOWN
        double mean = sumY / size;
        double relativeThreshold = 0.05; // 5% of mean — tune to your needs
        double threshold = mean * relativeThreshold;
        if (Math.abs(slope) <= threshold) {
            return TrendIndicator.STABLE;
        }
        return slope > 0 ? TrendIndicator.UP : TrendIndicator.DOWN;
    }
}
