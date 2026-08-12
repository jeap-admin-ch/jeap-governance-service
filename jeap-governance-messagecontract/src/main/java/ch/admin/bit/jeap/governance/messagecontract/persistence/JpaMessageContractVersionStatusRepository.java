package ch.admin.bit.jeap.governance.messagecontract.persistence;

import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaMessageContractVersionStatusRepository extends JpaRepository<MessageContractVersionStatus, Long> {
    List<MessageContractVersionStatus> findByAppNameAndUpToDateFalse(String appName);
}
