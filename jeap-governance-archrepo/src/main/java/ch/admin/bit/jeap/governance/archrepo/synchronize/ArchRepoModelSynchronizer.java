package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchRepoModelSynchronizer {

    private final ArchRepoModelSystemSynchronizer archRepoModelSystemSynchronizer;

    public void synchronizeWithArchRepo(ArchRepoModelDto archRepoModel) {
        List<ArchRepoSystemDto> archRepoSystems = getSystems(archRepoModel);
        Set<String> allArchRepoSystemNames = new HashSet<>();
        for (ArchRepoSystemDto archRepoSystem : archRepoSystems) {
            archRepoModelSystemSynchronizer.synchronizeWithArchRepo(archRepoSystem);
            allArchRepoSystemNames.add(archRepoSystem.getName());
        }

        archRepoModelSystemSynchronizer.deleteNoMoreExistingSystems(allArchRepoSystemNames);
    }

    private List<ArchRepoSystemDto> getSystems(ArchRepoModelDto archRepoModel) {
        if (archRepoModel.getSystems() == null) {
            return List.of();
        }
        return archRepoModel.getSystems().stream().filter(this::hasComponents).toList();
    }

    private boolean hasComponents(ArchRepoSystemDto system) {
        return system.getSystemComponents() != null && !system.getSystemComponents().isEmpty();
    }
}
