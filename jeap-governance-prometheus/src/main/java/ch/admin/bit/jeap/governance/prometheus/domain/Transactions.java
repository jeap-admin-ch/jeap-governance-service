package ch.admin.bit.jeap.governance.prometheus.domain;

public interface Transactions {

    void inNewTransaction(Runnable runnable);

}
