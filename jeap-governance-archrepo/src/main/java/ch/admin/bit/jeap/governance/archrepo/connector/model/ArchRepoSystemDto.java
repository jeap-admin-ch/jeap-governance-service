package ch.admin.bit.jeap.governance.archrepo.connector.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
public class ArchRepoSystemDto {
    String name;
    Set<String> aliases;
    List<ArchRepoSystemComponentDto> systemComponents;
}
