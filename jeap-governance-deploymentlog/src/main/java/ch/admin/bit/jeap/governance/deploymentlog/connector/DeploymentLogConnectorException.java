package ch.admin.bit.jeap.governance.deploymentlog.connector;

class DeploymentLogConnectorException extends RuntimeException {

    public DeploymentLogConnectorException(Throwable cause) {
        super("DeploymentLog API call failed", cause);
    }

}
