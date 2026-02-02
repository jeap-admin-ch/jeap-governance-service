package ch.admin.bit.jeap.governance.archrepo.connector.model;

import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class ReactionGraphDto {
    @NonNull
    String component;
    @NonNull
    ZonedDateTime lastModifiedAt;
}
