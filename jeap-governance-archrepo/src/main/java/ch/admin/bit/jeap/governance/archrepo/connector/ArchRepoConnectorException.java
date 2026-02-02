package ch.admin.bit.jeap.governance.archrepo.connector;

class ArchRepoConnectorException extends RuntimeException {

    public ArchRepoConnectorException(Throwable cause) {
        super("ArchRepo API call failed", cause);
    }

}
