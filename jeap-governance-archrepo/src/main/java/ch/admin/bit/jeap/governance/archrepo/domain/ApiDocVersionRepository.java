package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.Optional;
import java.util.UUID;

public interface ApiDocVersionRepository {

    Optional<ApiDocVersion> findByComponentId(UUID id);

    ApiDocVersion add(ApiDocVersion apiDocVersion);

    void delete(ApiDocVersion apiDocVersion);

    void deleteAllBySystemId(UUID systemId);
}
