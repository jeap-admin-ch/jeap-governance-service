package ch.admin.bit.jeap.governance.archrepo.connector.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArchRepoSystemComponentDto {
    String name;
    ArchRepoSystemComponentType type;
}
