package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ApiDocVersionRepositoryImpl implements ApiDocVersionRepository {

    private final JpaApiDocVersionRepository jpaRepository;

    @Override
    public Optional<ApiDocVersion> findByComponentId(UUID id) {
        return jpaRepository.findBySystemComponentId(id);
    }

    @Override
    public ApiDocVersion add(ApiDocVersion apiDocVersion) {
        return jpaRepository.save(apiDocVersion);
    }

    @Override
    public void delete(ApiDocVersion apiDocVersion) {
        jpaRepository.delete(apiDocVersion);
    }

    @Override
    public void deleteAllBySystemId(UUID systemId) {
        jpaRepository.deleteAllBySystemId(systemId);
    }
}
