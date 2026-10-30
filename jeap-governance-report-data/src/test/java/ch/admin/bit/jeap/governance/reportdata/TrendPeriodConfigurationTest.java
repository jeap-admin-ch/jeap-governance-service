package ch.admin.bit.jeap.governance.reportdata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrendPeriodConfigurationTest {

    @Test
    void withoutAnySetting_isThirtyDays() {
        assertThat(trendPeriodOf(new MockEnvironment())).isEqualTo(new TrendPeriod(30));
    }

    @Test
    void theNewProperty_setsIt() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("jeap.governance.report-data.trend-period-days", "14");

        assertThat(trendPeriodOf(environment)).isEqualTo(new TrendPeriod(14));
    }

    @Test
    void theDeprecatedAlias_setsIt() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("jeap.governance.reporting.trendPeriodDays", "7");

        assertThat(trendPeriodOf(environment)).isEqualTo(new TrendPeriod(7));
    }

    @Test
    void theDeprecatedAlias_winsOverTheNewProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("jeap.governance.report-data.trend-period-days", "14")
                .withProperty("jeap.governance.reporting.trend-period-days", "7");

        assertThat(trendPeriodOf(environment)).isEqualTo(new TrendPeriod(7));
    }

    @Test
    void fromDay_includesTheGivenNumberOfDays() {
        LocalDate untilDay = LocalDate.of(2026, 10, 31);

        assertThat(new TrendPeriod(30).fromDay(untilDay)).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(new TrendPeriod(1).fromDay(untilDay)).isEqualTo(untilDay);
    }

    @Test
    void lessThanOneDay_isRefused() {
        assertThatThrownBy(() -> new TrendPeriod(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static TrendPeriod trendPeriodOf(MockEnvironment environment) {
        ReportDataProperties properties = Binder.get(environment)
                .bindOrCreate("jeap.governance.report-data", ReportDataProperties.class);
        return new ReportDataAutoConfiguration().trendPeriod(properties, environment);
    }
}
