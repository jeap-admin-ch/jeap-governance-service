package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.Optional;

public interface DatabaseSchemaVersionRepository {

    Optional<DatabaseSchemaVersion> findByComponentId(Long id);

    DatabaseSchemaVersion add(DatabaseSchemaVersion apiDocVersion);

    void delete(DatabaseSchemaVersion apiDocVersion);

    void deleteAllBySystemId(Long systemId);
}
