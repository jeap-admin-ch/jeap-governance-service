package ch.admin.bit.jeap.governance.messagecontract.domain;

import java.util.List;

public interface MessageContractVersionStatusRepository {
    List<MessageContractVersionStatus> findOutdatedByAppName(String appName);

    void replaceSnapshot(List<MessageContractVersionStatus> statuses);
}
