package ch.admin.bit.jeap.governance.archrepo.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "RestApiRelationWithoutPact")
@Table(name = "ar_rest_api_relation_without_pact")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class RestApiRelationWithoutPact {
    @Id
    @NonNull
    @EqualsAndHashCode.Include
    @Getter
    private UUID id;

    @NonNull
    @Getter
    String method;

    @NonNull
    @Getter
    String path;

    @ManyToOne
    @Getter
    private SystemComponent providerSystemComponent;

    @ManyToOne
    @Getter
    private SystemComponent consumerSystemComponent;

    @Getter
    private ZonedDateTime createdAt;

    @Builder
    private static RestApiRelationWithoutPact build(UUID id, String method, String path, SystemComponent providerSystemComponent, SystemComponent consumerSystemComponent, ZonedDateTime createdAt) {
        return new RestApiRelationWithoutPact(id, method, path, providerSystemComponent, consumerSystemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
