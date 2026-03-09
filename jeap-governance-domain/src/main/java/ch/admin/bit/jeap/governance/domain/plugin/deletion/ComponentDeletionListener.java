package ch.admin.bit.jeap.governance.domain.plugin.deletion;

/**
 * This listener is invoked before the component deletion is finalized, allowing modules
 * to remove associated data or perform other cleanup tasks.
 */
public interface ComponentDeletionListener {

    /**
     * This method is called before a component identified by {@code systemComponentId} is deleted.
     * Implementations should perform any necessary cleanup or data removal related to the component.
     *
     * @param systemComponentId the unique identifier of the component to be deleted
     */
    void preComponentDeletion(long systemComponentId);

}
