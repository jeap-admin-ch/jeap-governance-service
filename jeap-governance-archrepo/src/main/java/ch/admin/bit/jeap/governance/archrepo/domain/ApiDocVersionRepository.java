package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.Optional;

public interface ApiDocVersionRepository {

    Optional<ApiDocVersion> findByComponentId(Long id);

    ApiDocVersion add(ApiDocVersion apiDocVersion);

    void delete(ApiDocVersion apiDocVersion);

    void deleteAllBySystemId(Long systemId);
}
