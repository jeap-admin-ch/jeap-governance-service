package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceUnavailableException;
import ch.admin.bit.jeap.governance.domain.scheduler.JobStopSignal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataImporter {

    private final List<DataSourceImporter> dataSourceImporters;
    private final MeterRegistry meterRegistry;
    private final JobStopSignal stopSignal;

    @PostConstruct
    void init() {
        String allConnectors = dataSourceImporters.stream()
                .map(dataSourceConnectorImport -> dataSourceConnectorImport.getClass().getName())
                .collect(Collectors.joining(", "));
        log.info("Registered DataSourceConnectorImports: {}", allConnectors);
    }

    /**
     * Runs every importer in turn, or stops before the next one once the service is shutting down.
     *
     * @return true if the import ran to its end, false if it stopped because the service is shutting down
     */
    public boolean importData() {
        log.info("Importing governance data...");

        for (int i = 0; i < dataSourceImporters.size(); i++) {
            if (stopSignal.isStopRequested()) {
                log.warn("Importing governance data stopped as the service is shutting down, {} of {} importers not run",
                        dataSourceImporters.size() - i, dataSourceImporters.size());
                return false;
            }
            importData(dataSourceImporters.get(i));
        }

        // The last importer may itself have stopped early
        if (stopSignal.isStopRequested()) {
            log.warn("Importing governance data stopped as the service is shutting down");
            return false;
        }
        log.info("Importing governance data done");
        return true;
    }

    private void importData(DataSourceImporter dataSourceImporter) {
        Timer.Sample sample = Timer.start(meterRegistry);

        boolean success = doImport(dataSourceImporter);

        sample.stop(meterRegistry.timer(
                "jeap_governance_service_data_import_duration",
                "data_source_connector", dataSourceImporter.getClass().getSimpleName(),
                "success", String.valueOf(success)
        ));
    }

    private static boolean doImport(DataSourceImporter dataSourceImporter) {
        try {
            log.debug("Import data with {}...", dataSourceImporter.getClass().getSimpleName());
            dataSourceImporter.importData();
            log.debug("Import data done");
            return true;
        } catch (DataSourceUnavailableException e) {
            log.warn("Import data with {} skipped, previous data retained: {}",
                    dataSourceImporter.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Import data with {} failed", dataSourceImporter.getClass().getSimpleName(), e);
            return false;
        }
    }

}
