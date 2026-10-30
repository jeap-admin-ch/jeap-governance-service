package ch.admin.bit.jeap.governance.reportdata;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jeap.governance.report-data")
public class ReportDataProperties {

    /**
     * Number of days the trends are calculated over, and the length of the score and conformance histories.
     */
    private int trendPeriodDays = 30;
}
