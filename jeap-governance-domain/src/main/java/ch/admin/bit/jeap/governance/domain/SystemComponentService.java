package ch.admin.bit.jeap.governance.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemComponentService {

    private final ComponentDeletionListenerDelegate deletionListenerDelegate;
    private final SystemComponentRepository systemComponentRepository;

    public void deleteById(Long systemComponentId) {
        deletionListenerDelegate.notifyPreComponentDeletion(systemComponentId);
        systemComponentRepository.deleteById(systemComponentId);
    }

}
