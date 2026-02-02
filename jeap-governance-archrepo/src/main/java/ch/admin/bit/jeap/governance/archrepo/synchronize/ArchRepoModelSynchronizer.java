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

    public void synchronizeModelWithArchRepo(ArchRepoModelDto archRepoModel) {
        List<ArchRepoSystemDto> archRepoSystems = archRepoModel.getSystems();
        Set<String> allArchRepoSystemNames = new HashSet<>();
        for (ArchRepoSystemDto archRepoSystem : archRepoSystems) {
            archRepoModelSystemSynchronizer.synchronizeSystemWithArchRepo(archRepoSystem);
            allArchRepoSystemNames.add(archRepoSystem.getName());
        }

        archRepoModelSystemSynchronizer.deleteNoMoreExistingSystems(allArchRepoSystemNames);
    }
}
