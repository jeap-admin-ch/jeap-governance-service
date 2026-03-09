package ch.admin.bit.jeap.governance.domain.plugin.deletion;

/**
 * This listener is invoked before the system deletion is finalized, allowing modules
 * to remove associated data or perform other cleanup tasks.
 */
public interface SystemDeletionListener {

    /**
     * This method is called before a system identified by {@code systemId} is deleted.
     * Implementations should perform any necessary cleanup or data removal related to the system.
     *
     * @param systemId the unique identifier of the system to be deleted
     */
    void preSystemDeletion(long systemId);

}
