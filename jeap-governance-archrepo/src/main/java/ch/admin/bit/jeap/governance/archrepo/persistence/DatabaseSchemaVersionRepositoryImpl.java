package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DatabaseSchemaVersionRepositoryImpl implements DatabaseSchemaVersionRepository {

    private final JpaDatabaseSchemaVersionRepository jpaRepository;

    @Override
    public Optional<DatabaseSchemaVersion> findByComponentId(UUID id) {
        return jpaRepository.findBySystemComponentId(id);
    }

    @Override
    public DatabaseSchemaVersion add(DatabaseSchemaVersion databaseSchemaVersion) {
        return jpaRepository.save(databaseSchemaVersion);
    }

    @Override
    public void delete(DatabaseSchemaVersion databaseSchemaVersion) {
        jpaRepository.delete(databaseSchemaVersion);
    }

    @Override
    public void deleteAllBySystemId(UUID systemId) {
        jpaRepository.deleteAllBySystemId(systemId);
    }
}
