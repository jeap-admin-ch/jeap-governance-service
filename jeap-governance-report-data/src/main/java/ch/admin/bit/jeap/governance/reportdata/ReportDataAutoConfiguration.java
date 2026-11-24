package ch.admin.bit.jeap.governance.reportdata;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * The report data of the docs API: scores, trends, histories, rule states and conformance.
 */
@AutoConfiguration
@ComponentScan
@EnableConfigurationProperties(ReportDataProperties.class)
public class ReportDataAutoConfiguration {

    @Bean
    TrendPeriod trendPeriod(ReportDataProperties properties) {
        return new TrendPeriod(properties.getTrendPeriodDays());
    }
}
