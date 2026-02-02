package ch.admin.bit.jeap.governance.archrepo.connector.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ArchRepoModelDto {
    List<ArchRepoSystemDto> systems;
}
