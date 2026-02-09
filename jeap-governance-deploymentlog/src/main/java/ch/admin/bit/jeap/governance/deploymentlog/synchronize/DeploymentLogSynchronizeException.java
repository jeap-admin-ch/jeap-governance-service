package ch.admin.bit.jeap.governance.deploymentlog.synchronize;

class DeploymentLogSynchronizeException extends RuntimeException {

    public DeploymentLogSynchronizeException(String message) {
        super("DeploymentLog import failed:" + message);
    }

}
