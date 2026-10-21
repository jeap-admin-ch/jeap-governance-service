package ch.admin.bit.jeap.governance.domain.plugin.datasource;

/**
 * Thrown by a {@link DataSourceImporter} when its data source is temporarily unable to deliver reliable data, e.g.
 * because it answered with HTTP 503. The importer must leave its previously imported data untouched; the next
 * scheduled import retries. The import is reported as failed, but logged as a warning instead of an error because
 * this is an expected, self-healing condition rather than a defect.
 */
public class DataSourceUnavailableException extends RuntimeException {

    public DataSourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
