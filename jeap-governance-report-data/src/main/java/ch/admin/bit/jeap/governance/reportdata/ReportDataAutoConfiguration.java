package ch.admin.bit.jeap.governance.reportdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

/**
 * The report data is always present: both the Confluence reporting and the docs API read it, so that both show
 * the same values.
 */
@AutoConfiguration
@ComponentScan
@EnableConfigurationProperties(ReportDataProperties.class)
@Slf4j
public class ReportDataAutoConfiguration {

    /**
     * The trend period of release 9.17 and before, kept as a deprecated alias of
     * {@code jeap.governance.report-data.trend-period-days}. It wins when set, so that an instance which set it
     * keeps its period.
     */
    static final String DEPRECATED_TREND_PERIOD_DAYS = "jeap.governance.reporting.trend-period-days";

    @Bean
    TrendPeriod trendPeriod(ReportDataProperties properties, Environment environment) {
        return Binder.get(environment).bind(DEPRECATED_TREND_PERIOD_DAYS, Integer.class)
                .map(days -> {
                    log.warn("'{}' is deprecated, use 'jeap.governance.report-data.trend-period-days' instead",
                            DEPRECATED_TREND_PERIOD_DAYS);
                    return new TrendPeriod(days);
                })
                .orElseGet(() -> new TrendPeriod(properties.getTrendPeriodDays()));
    }
}
