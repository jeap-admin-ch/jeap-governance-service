package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.secscan.domain.SecscanTransactions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringSecscanSecscanTransactions implements SecscanTransactions {

    private final TransactionTemplate transactionTemplate;

    public SpringSecscanSecscanTransactions(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void inNewTransaction(Runnable runnable) {
        transactionTemplate.executeWithoutResult(status -> runnable.run());
    }

}
