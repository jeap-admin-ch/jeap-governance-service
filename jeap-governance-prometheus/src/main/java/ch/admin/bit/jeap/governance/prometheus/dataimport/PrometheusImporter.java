package ch.admin.bit.jeap.governance.prometheus.dataimport;

import ch.admin.bit.jeap.governance.domain.GovernanceProperties;
import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.prometheus.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusImporter implements DataSourceImporter {

    private final GovernanceProperties governanceProperties;
    private final PromClient promClient;
    private final PromTimeSeriesRepository promTimeSeriesRepository;
    private final Transactions transactions;
    private final SystemComponentRepository systemComponentRepository;

    @Override
    public void importData() {
        GovernanceServiceEnvironment environment = governanceProperties.getEnvironment();
        log.info("Starting import of Prometheus data from environment '{}'.", environment);
        systemComponentRepository.findAllSystemComponentNames().forEach(systemComponentName -> {
            try {
                importDataForSystemComponent(systemComponentName, environment);
            } catch (Exception e) {
                log.error("Failed to import new data from Prometheus for the environment '{}' and the system component '{}'. " +
                        "Leaving the existing data unchanged.", environment, systemComponentName, e);
            }
        });
        log.info("Finished import of Prometheus data from environment '{}'.", environment);
    }

    private void importDataForSystemComponent(String systemComponentName, GovernanceServiceEnvironment environment) {
        List<PromTimeSeries> timeSeriesList = getDataForSystemComponent(systemComponentName, environment);
        // Update the data of each service in a separate transaction to avoid long-running transactions and memory issues
        transactions.inNewTransaction(() ->
                updateDataForSystemComponent(timeSeriesList, systemComponentName)
        );
    }

    private List<PromTimeSeries> getDataForSystemComponent(String systemComponentName, GovernanceServiceEnvironment environment) {
        log.debug("Querying Prometheus for the environment '{}' and the system component '{}'.", environment, systemComponentName);
        List<PromTimeSeries> timeSeriesList = new ArrayList<>();
        int numFailedQueries = 0;
        for (PromQueryType queryType : PromQueryType.values()) {
            try {
                timeSeriesList.addAll(queryPrometheus(queryType, environment, systemComponentName));
            } catch (Exception e) {
                numFailedQueries++;
                log.error("Failed to query Prometheus for the query type '{}', the environment '{}' and the system component '{}'.",
                    queryType, environment, systemComponentName, e);
            }
        }
        if (numFailedQueries == PromQueryType.values().length) {
            throw PromException.allQueryTypesFailedForSystemComponent(systemComponentName, environment);
        } else if (numFailedQueries > 0) {
            log.warn("Some queries to Prometheus failed for the environment '{}' and the system component '{}'. " +
                    "Number of failed queries: {} out of {}.",
                environment, systemComponentName, numFailedQueries, PromQueryType.values().length);
        }
        return timeSeriesList;
    }

    private List<PromTimeSeries> queryPrometheus(PromQueryType queryType, GovernanceServiceEnvironment environment, String systemComponentName) {
        ZonedDateTime now = ZonedDateTime.now();
        return promClient.query(queryType, environment, systemComponentName).stream().
                map(sample ->
                    PromTimeSeries.builder()
                        .prometheusQueryType(queryType)
                        .queryTimestamp(now)
                        .systemComponentName(systemComponentName)
                        .sample(sample)
                        .build())
                .toList();
    }

    private void updateDataForSystemComponent(List<PromTimeSeries> timeSeriesList, String systemComponentName) {
        log.debug("Updating Prometheus data for the system component '{}' with {} time series.", systemComponentName, timeSeriesList.size());
        // Assuming isolation level READ COMMITTED for queries to the repository
        log.debug("Deleting all time series for the system component '{}' for the data update.", systemComponentName);
        promTimeSeriesRepository.deleteBy(systemComponentName);
        if (!timeSeriesList.isEmpty()) {
            log.info("Writing {} new time series for the system component '{}'.", timeSeriesList.size(), systemComponentName);
            promTimeSeriesRepository.saveAll(timeSeriesList);
        }
        log.debug("Updated Prometheus data for '{}'.", systemComponentName);
    }

}
