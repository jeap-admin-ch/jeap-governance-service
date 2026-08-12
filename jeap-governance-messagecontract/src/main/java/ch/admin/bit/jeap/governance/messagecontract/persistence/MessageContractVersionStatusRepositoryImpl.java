package ch.admin.bit.jeap.governance.messagecontract.persistence;

import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageContractVersionStatusRepositoryImpl implements MessageContractVersionStatusRepository {
    private final JpaMessageContractVersionStatusRepository statusRepository;

    @Override
    public List<MessageContractVersionStatus> findOutdatedByAppName(String appName) {
        return statusRepository.findByAppNameAndUpToDateFalse(appName);
    }

    @Override
    @Transactional
    public void replaceSnapshot(List<MessageContractVersionStatus> statuses) {
        statusRepository.deleteAllInBatch();
        statusRepository.saveAll(statuses);
    }
}
