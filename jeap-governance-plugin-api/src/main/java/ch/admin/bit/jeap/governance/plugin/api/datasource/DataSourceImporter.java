package ch.admin.bit.jeap.governance.plugin.api.datasource;

/**
 * Interface for data source connectors used in the governance service.
 * <p>
 * Implementations provide integration with external data sources for governance
 * data management including import operations.
 *
 * <h2>Implementation Requirements</h2>
 * <ul>
 *   <li>Must be registered as a Spring bean (e.g., via {@code @Component} or {@code @Service})</li>
 *   <li>Should handle connection lifecycle</li>
 *   <li>Should be annotated with {@code org.springframework.core.annotation.Order} to define the sort order</li>
 *   <li>Transaction handling must be done within the implementation if needed</li>
 * </ul>
 *
 */
public interface DataSourceImporter {

    /**
     * Imports data from the external data source into the governance system.
     */
    void importData();

}
