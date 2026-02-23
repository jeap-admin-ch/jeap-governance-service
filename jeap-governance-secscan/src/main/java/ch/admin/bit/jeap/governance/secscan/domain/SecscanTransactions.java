package ch.admin.bit.jeap.governance.secscan.domain;

public interface SecscanTransactions {

    void inNewTransaction(Runnable runnable);

}
