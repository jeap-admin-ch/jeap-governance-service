package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.StreamSupport;

@Repository
@RequiredArgsConstructor
class SecscanFlaggedEndpointRepositoryImpl implements SecscanFlaggedEndpointRepository {

    private final SecscanFlaggedEndpointJpaRepository secscanFlaggedEndpointJpaRepository;

    @Override
    public List<SecscanFlaggedEndpoint> saveAll(List<SecscanFlaggedEndpoint> flaggedEndpoints) {
        Iterable<SecscanFlaggedEndpoint> savedFlaggedEndpoints = secscanFlaggedEndpointJpaRepository.saveAll(flaggedEndpoints);
        return StreamSupport.stream(savedFlaggedEndpoints.spliterator(), false).toList();
    }

    @Override
    public List<SecscanFlaggedEndpoint> findBySystemComponentId(long systemComponentId) {
        return secscanFlaggedEndpointJpaRepository.findBySystemComponentId(systemComponentId);
    }

    @Override
    public int deleteBySystemComponentId(long systemComponentId) {
        return secscanFlaggedEndpointJpaRepository.deleteBySystemComponentId(systemComponentId);
    }

}
