package ch.admin.bit.jeap.governance.plugin.api.datasource;

/**
 * Interface for data source connectors used in the governance service.
 * <p>
 * This listener is invoked before the component deletion is finalized, allowing connectors
 * to remove associated data, close connections, or perform other cleanup tasks.
 */
public interface ComponentDeletionListener {

    /**
     * This method is called before a component identified by {@code systemComponentId} is deleted.
     * Implementations should perform any necessary cleanup or data removal related to the component.
     *
     * @param systemComponentId the unique identifier of the component to be deleted
     */
    void preComponentDeletion(Long systemComponentId);

}
