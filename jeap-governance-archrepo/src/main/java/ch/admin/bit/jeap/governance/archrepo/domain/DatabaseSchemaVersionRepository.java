package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.Optional;
import java.util.UUID;

public interface DatabaseSchemaVersionRepository {

    Optional<DatabaseSchemaVersion> findByComponentId(UUID id);

    DatabaseSchemaVersion add(DatabaseSchemaVersion apiDocVersion);

    void delete(DatabaseSchemaVersion apiDocVersion);

    void deleteAllBySystemId(UUID systemId);
}
