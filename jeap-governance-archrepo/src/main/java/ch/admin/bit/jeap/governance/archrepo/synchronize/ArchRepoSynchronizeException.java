package ch.admin.bit.jeap.governance.archrepo.synchronize;

class ArchRepoSynchronizeException extends RuntimeException {

    public ArchRepoSynchronizeException(String message) {
        super("ArchRepo import failed:" + message);
    }

}
