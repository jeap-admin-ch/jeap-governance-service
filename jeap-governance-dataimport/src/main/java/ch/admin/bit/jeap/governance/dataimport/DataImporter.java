package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
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

    @PostConstruct
    void init() {
        String allConnectors = dataSourceImporters.stream()
                .map(dataSourceConnectorImport -> dataSourceConnectorImport.getClass().getName())
                .collect(Collectors.joining(", "));
        log.info("Registered DataSourceConnectorImports: {}", allConnectors);
    }

    public void importData() {
        log.info("Importing governance data...");

        dataSourceImporters.forEach(this::importData);

        log.info("Importing governance data done");
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
            log.debug("Import data with {}...", dataSourceImporter.getClass());
            dataSourceImporter.importData();
            log.debug("Import data done");
            return true;
        } catch (Exception e) {
            log.error("Import data with {} failed", dataSourceImporter.getClass(), e);
            return false;
        }
    }

}
