package ch.admin.bit.jeap.governance.reactionobserver.synchronize;

class ReactionObserverSynchronizeException extends RuntimeException {

    public ReactionObserverSynchronizeException(String message) {
        super("ReactionObserver synchronization failed:" + message);
    }
}
