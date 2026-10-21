package ch.admin.bit.jeap.governance.messagecontract.domain;

import java.util.List;

public interface MessageContractVersionStatusRepository {
    /**
     * @return the contracts of the application that are not known to be up to date, i.e. with state
     * {@link MessageContractVersionState#OUTDATED} or {@link MessageContractVersionState#UNKNOWN}
     */
    List<MessageContractVersionStatus> findNotUpToDateByAppName(String appName);

    void replaceSnapshot(List<MessageContractVersionStatus> statuses);
}
