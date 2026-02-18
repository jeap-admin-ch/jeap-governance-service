package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.Optional;

public interface ApiDocVersionRepository {

    Optional<ApiDocVersion> findByComponentId(long id);

    ApiDocVersion add(ApiDocVersion apiDocVersion);

    void delete(ApiDocVersion apiDocVersion);

    void deleteAllBySystemId(long systemId);
}
