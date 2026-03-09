package ch.admin.bit.jeap.governance.reactionobserver.connector;

class ReactionObserverConnectorException extends RuntimeException {

    public ReactionObserverConnectorException(Throwable cause) {
        super("ReactionObserver API call failed", cause);
    }

}
