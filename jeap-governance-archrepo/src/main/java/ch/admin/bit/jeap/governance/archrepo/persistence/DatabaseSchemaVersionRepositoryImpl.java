package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DatabaseSchemaVersionRepositoryImpl implements DatabaseSchemaVersionRepository {

    private final JpaDatabaseSchemaVersionRepository jpaRepository;

    @Override
    public Optional<DatabaseSchemaVersion> findByComponentId(long id) {
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
    public void deleteAllBySystemId(long systemId) {
        jpaRepository.deleteAllBySystemId(systemId);
    }
}
