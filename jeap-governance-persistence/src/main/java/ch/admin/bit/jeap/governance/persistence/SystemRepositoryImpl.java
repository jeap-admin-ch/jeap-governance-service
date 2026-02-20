package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemReference;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SystemRepositoryImpl implements SystemRepository {

    private final JpaSystemRepository jpaSystemRepository;

    @Override
    public List<System> findAll() {
        return jpaSystemRepository.findAll();
    }

    @Override
    public List<Long> findAllIds() {
        return jpaSystemRepository.findAllIds();
    }

    @Override
    public Optional<System> findByName(String systemName) {
        return jpaSystemRepository.findByName(systemName);
    }

    @Override
    public Optional<System> findById(Long id) {
        return jpaSystemRepository.findById(id);
    }

    @Override
    public System add(System system) {
        return jpaSystemRepository.save(system);
    }

    @Override
    public void update(System system) {
        jpaSystemRepository.save(system);
    }

    @Override
    @Transactional
    public void delete(System system) {
        jpaSystemRepository.delete(system);
    }

    @Override
    public List<SystemReference> findAllSystemReferences() {
        return jpaSystemRepository.findAllSystemReferences();
    }
}
